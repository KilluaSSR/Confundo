cmake_minimum_required(VERSION 3.22.1)

project(confundo)

find_package(shadowhook REQUIRED CONFIG)

add_library(confundo SHARED confundo_native.cpp)

target_link_libraries(
        confundo
        shadowhook::shadowhook
        log
)
