cmake_minimum_required(VERSION 3.22.1)
project("talkify_sonic")

add_library(sonic SHARED sonic.c sonic_jni.c)

target_link_libraries(sonic log)
