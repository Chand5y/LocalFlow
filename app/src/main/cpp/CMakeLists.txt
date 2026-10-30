cmake_minimum_required(VERSION 3.22.1)
project("localflow_jni")

# Snapdragon 870 (Kryo 585) supports ARMv8.2-A DotProd and FP16 acceleration
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math")

add_library(
    localflow_jni
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    localflow_jni
    ${log-lib}
)
