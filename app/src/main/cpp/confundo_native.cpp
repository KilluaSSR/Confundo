#include <jni.h>
#include <android/log.h>
#include <fcntl.h>
#include <sys/system_properties.h>
#include <sys/utsname.h>
#include <unistd.h>
#include <vulkan/vulkan_core.h>

#include <atomic>
#include <cstring>
#include <fstream>
#include <mutex>
#include <sstream>
#include <string>
#include <unordered_map>
#include <vector>

#include "shadowhook.h"

#define LOG_TAG "ConfundoNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace {

std::unordered_map<std::string, std::string> g_props;
std::string g_cache_dir;           // 目标 App 可写缓存目录，用于生成改写后的文件并重定向
std::string g_kernel;              // 内核版本，用于 uname / /proc/version
std::string g_vulkan_device_name;  // 与 GL_RENDERER 对齐的 Vulkan deviceName
std::atomic<bool> g_installed{false};

thread_local bool tls_bypass = false;

const char* kMapsHideMarkers[] = {
        "libconfundo", "libshadowhook", "confundo_lib", "confundo_native",
        "shadowhook", "riru", "zygisk", "lsplant", "lsposed", "xposed", "edxp",
};

const std::string* LookupProp(const char* name) {
    if (name == nullptr) return nullptr;
    auto it = g_props.find(name);
    return it == g_props.end() ? nullptr : &it->second;
}

void CopyStr(char* dst, const std::string& src, size_t cap) {
    if (dst == nullptr || cap == 0) return;
    size_t n = src.size() < (cap - 1) ? src.size() : (cap - 1);
    memcpy(dst, src.c_str(), n);
    dst[n] = '\0';
}

std::string ReadRealFile(const char* path) {
    tls_bypass = true;
    std::ifstream in(path);
    std::stringstream ss;
    if (in) ss << in.rdbuf();
    tls_bypass = false;
    return ss.str();
}

// 把给定内容写入一次性临时文件并打开，随后立刻 unlink（fd 仍有效），返回只读 fd；失败返回 -1。
int MaterializeFd(const std::string& content) {
    if (g_cache_dir.empty()) return -1;
    std::string tmpl = g_cache_dir + "/redir.XXXXXX";
    std::vector<char> buf(tmpl.begin(), tmpl.end());
    buf.push_back('\0');
    tls_bypass = true;
    int fd = mkstemp(buf.data());
    if (fd >= 0) {
        const char* p = content.data();
        size_t left = content.size();
        while (left > 0) {
            ssize_t w = write(fd, p, left);
            if (w <= 0) break;
            p += w;
            left -= static_cast<size_t>(w);
        }
        lseek(fd, 0, SEEK_SET);
        unlink(buf.data());
    }
    tls_bypass = false;
    return fd;
}

// ---- __system_property_get ----------------------------------------------
using PropGetFn = int (*)(const char*, char*);
PropGetFn orig_property_get = nullptr;

int MyPropertyGet(const char* name, char* value) {
    SHADOWHOOK_STACK_SCOPE();  // SHARED 模式必需：清理 hub 栈状态，否则 proxy 仅生效一次后被判为递归而跳过。
    if (const std::string* spoof = LookupProp(name)) {
        size_t n = spoof->size();
        if (n > PROP_VALUE_MAX - 1) n = PROP_VALUE_MAX - 1;
        if (value != nullptr) {
            memcpy(value, spoof->c_str(), n);
            value[n] = '\0';
        }
        return static_cast<int>(n);
    }
    if (orig_property_get != nullptr) return orig_property_get(name, value);
    if (value != nullptr) value[0] = '\0';
    return 0;
}

// ---- __system_property_read --------
using PropReadFn = int (*)(const prop_info*, char*, char*);
PropReadFn orig_prop_read = nullptr;

int MyPropRead(const prop_info* pi, char* name, char* value) {
    SHADOWHOOK_STACK_SCOPE();
    // 常见用法是 find() + read(pi, nullptr, value)：调用方已知属性名，故传 name=nullptr。
    // 此时若不补一个自己的 name 缓冲区去回收属性名，就无法匹配 g_props，会漏出真实值。
    char name_buf[PROP_NAME_MAX];
    char* name_ptr = name != nullptr ? name : name_buf;
    if (name == nullptr) name_buf[0] = '\0';
    int r = orig_prop_read != nullptr ? orig_prop_read(pi, name_ptr, value) : -1;
    if (r >= 0) {
        if (const std::string* spoof = LookupProp(name_ptr)) {
            CopyStr(value, *spoof, PROP_VALUE_MAX);
            return static_cast<int>(spoof->size());
        }
    }
    return r;
}

// ---- __system_property_read_callback -------------------------------------
using PropReadCbFn = void (*)(const prop_info*,
                              void (*)(void*, const char*, const char*, uint32_t),
                              void*);
PropReadCbFn orig_prop_read_cb = nullptr;

struct ReadCbCtx {
    void (*callback)(void*, const char*, const char*, uint32_t);
    void* cookie;
};

void ReadCbTrampoline(void* cookie, const char* name, const char* value, uint32_t serial) {
    auto* ctx = static_cast<ReadCbCtx*>(cookie);
    const char* effective = value;
    if (const std::string* spoof = LookupProp(name)) effective = spoof->c_str();
    ctx->callback(ctx->cookie, name, effective, serial);
}

void MyPropReadCallback(const prop_info* pi,
                        void (*callback)(void*, const char*, const char*, uint32_t),
                        void* cookie) {
    SHADOWHOOK_STACK_SCOPE();
    if (orig_prop_read_cb == nullptr) return;
    ReadCbCtx ctx{callback, cookie};
    orig_prop_read_cb(pi, ReadCbTrampoline, &ctx);
}

// ---- uname（native 直读内核版本 / 主机名） --------------------------------
using UnameFn = int (*)(struct utsname*);
UnameFn orig_uname = nullptr;

int MyUname(struct utsname* buf) {
    SHADOWHOOK_STACK_SCOPE();
    int r = orig_uname != nullptr ? orig_uname(buf) : -1;
    if (r == 0 && buf != nullptr) {
        if (!g_kernel.empty()) CopyStr(buf->release, g_kernel, sizeof(buf->release));
        CopyStr(buf->nodename, "localhost", sizeof(buf->nodename));
    }
    return r;
}

// ---- openat（重定向 build.prop / /proc/version，过滤 /proc/self/maps） -----
using OpenatFn = int (*)(int, const char*, int, mode_t);
OpenatFn orig_openat = nullptr;

bool EndsWith(const char* s, const char* suffix) {
    size_t ls = strlen(s), lf = strlen(suffix);
    return ls >= lf && strcmp(s + (ls - lf), suffix) == 0;
}

bool IsSelfMaps(const char* path) {
    if (strcmp(path, "/proc/self/maps") == 0) return true;
    // /proc/<pid>/maps，其中 pid == 本进程
    char self[64];
    snprintf(self, sizeof(self), "/proc/%d/maps", getpid());
    return strcmp(path, self) == 0;
}

std::string BuildSpoofedProp(const char* path) {
    std::string content = ReadRealFile(path);
    if (content.empty()) return {};
    std::stringstream in(content);
    std::stringstream out;
    std::string line;
    while (std::getline(in, line)) {
        size_t eq = line.find('=');
        if (eq != std::string::npos && eq > 0) {
            auto it = g_props.find(line.substr(0, eq));
            if (it != g_props.end()) {
                out << line.substr(0, eq) << '=' << it->second << '\n';
                continue;
            }
        }
        out << line << '\n';
    }
    return out.str();
}

std::string BuildSpoofedProcVersion() {
    std::string content = ReadRealFile("/proc/version");
    if (content.empty() || g_kernel.empty()) return {};
    const std::string prefix = "Linux version ";
    size_t pos = content.find(prefix);
    if (pos == std::string::npos) return {};
    size_t start = pos + prefix.size();
    size_t end = content.find(' ', start);
    if (end == std::string::npos) return {};
    return content.substr(0, start) + g_kernel + content.substr(end);
}

std::string BuildFilteredMaps(const char* path) {
    std::string content = ReadRealFile(path);
    if (content.empty()) return {};
    std::stringstream in(content);
    std::stringstream out;
    std::string line;
    while (std::getline(in, line)) {
        bool hide = false;
        for (const char* marker : kMapsHideMarkers) {
            if (line.find(marker) != std::string::npos) {
                hide = true;
                break;
            }
        }
        if (!hide) out << line << '\n';
    }
    return out.str();
}

int MyOpenat(int dirfd, const char* pathname, int flags, mode_t mode) {
    SHADOWHOOK_STACK_SCOPE();
    if (!tls_bypass && pathname != nullptr) {
        std::string content;
        if (EndsWith(pathname, "/build.prop")) {
            content = BuildSpoofedProp(pathname);
        } else if (strcmp(pathname, "/proc/version") == 0) {
            content = BuildSpoofedProcVersion();
        } else if (IsSelfMaps(pathname)) {
            content = BuildFilteredMaps(pathname);
        }
        if (!content.empty()) {
            int fd = MaterializeFd(content);
            if (fd >= 0) return fd;
        }
    }
    return orig_openat(dirfd, pathname, flags, mode);
}

// ---- Vulkan deviceName ----------------------------------------------------
using VkGetPropsFn = void (*)(VkPhysicalDevice, VkPhysicalDeviceProperties*);
VkGetPropsFn orig_vk_get_props = nullptr;

void OverwriteVkName(char* device_name) {
    if (device_name == nullptr || g_vulkan_device_name.empty()) return;
    CopyStr(device_name, g_vulkan_device_name, VK_MAX_PHYSICAL_DEVICE_NAME_SIZE);
}

void MyVkGetProps(VkPhysicalDevice device, VkPhysicalDeviceProperties* props) {
    SHADOWHOOK_STACK_SCOPE();
    if (orig_vk_get_props != nullptr) orig_vk_get_props(device, props);
    if (props != nullptr) OverwriteVkName(props->deviceName);
}

using VkGetProps2Fn = void (*)(VkPhysicalDevice, VkPhysicalDeviceProperties2*);
VkGetProps2Fn orig_vk_get_props2 = nullptr;

void MyVkGetProps2(VkPhysicalDevice device, VkPhysicalDeviceProperties2* props) {
    SHADOWHOOK_STACK_SCOPE();
    if (orig_vk_get_props2 != nullptr) orig_vk_get_props2(device, props);
    if (props != nullptr) OverwriteVkName(props->properties.deviceName);
}

void HookSym(const char* lib, const char* sym, void* proxy, void** orig) {
    void* stub = shadowhook_hook_sym_name(lib, sym, proxy, orig);
    if (stub == nullptr) {
        LOGW("hook failed: %s!%s (errno=%d)", lib, sym, shadowhook_get_errno());
    }
}

void InstallHooks() {
    HookSym("libc.so", "__system_property_get",
            reinterpret_cast<void*>(MyPropertyGet),
            reinterpret_cast<void**>(&orig_property_get));
    HookSym("libc.so", "__system_property_read",
            reinterpret_cast<void*>(MyPropRead),
            reinterpret_cast<void**>(&orig_prop_read));
    HookSym("libc.so", "__system_property_read_callback",
            reinterpret_cast<void*>(MyPropReadCallback),
            reinterpret_cast<void**>(&orig_prop_read_cb));
    HookSym("libc.so", "uname",
            reinterpret_cast<void*>(MyUname),
            reinterpret_cast<void**>(&orig_uname));
    HookSym("libc.so", "openat",
            reinterpret_cast<void*>(MyOpenat),
            reinterpret_cast<void**>(&orig_openat));

    if (!g_vulkan_device_name.empty()) {
        HookSym("libvulkan.so", "vkGetPhysicalDeviceProperties",
                reinterpret_cast<void*>(MyVkGetProps),
                reinterpret_cast<void**>(&orig_vk_get_props));
        HookSym("libvulkan.so", "vkGetPhysicalDeviceProperties2",
                reinterpret_cast<void*>(MyVkGetProps2),
                reinterpret_cast<void**>(&orig_vk_get_props2));
        HookSym("libvulkan.so", "vkGetPhysicalDeviceProperties2KHR",
                reinterpret_cast<void*>(MyVkGetProps2),
                reinterpret_cast<void**>(&orig_vk_get_props2));
    }
}

std::string JStringToStd(JNIEnv* env, jstring s) {
    if (s == nullptr) return {};
    const char* chars = env->GetStringUTFChars(s, nullptr);
    std::string result = chars != nullptr ? chars : "";
    if (chars != nullptr) env->ReleaseStringUTFChars(s, chars);
    return result;
}

}  // namespace

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    shadowhook_init(SHADOWHOOK_MODE_SHARED, false);
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_killua_dev_confundo_hooks_NativeBridge_nativeInstall(
        JNIEnv* env, jclass /*clazz*/,
        jobjectArray keys, jobjectArray values,
        jstring cacheDir, jstring kernel, jstring vulkanDeviceName) {
    bool expected = false;
    if (!g_installed.compare_exchange_strong(expected, true)) {
        return JNI_TRUE;  // 已安装，幂等返回。
    }

    g_cache_dir = JStringToStd(env, cacheDir);
    g_kernel = JStringToStd(env, kernel);
    g_vulkan_device_name = JStringToStd(env, vulkanDeviceName);

    jsize count = keys != nullptr ? env->GetArrayLength(keys) : 0;
    for (jsize i = 0; i < count; ++i) {
        auto key = reinterpret_cast<jstring>(env->GetObjectArrayElement(keys, i));
        auto value = reinterpret_cast<jstring>(env->GetObjectArrayElement(values, i));
        std::string k = JStringToStd(env, key);
        std::string v = JStringToStd(env, value);
        if (!k.empty()) g_props[k] = v;
        if (key != nullptr) env->DeleteLocalRef(key);
        if (value != nullptr) env->DeleteLocalRef(value);
    }

    InstallHooks();
    LOGI("native install done, %zu props", g_props.size());
    return JNI_TRUE;
}
