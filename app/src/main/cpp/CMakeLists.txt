cmake_minimum_required(VERSION 3.10.2)

project(memoryreader LANGUAGES CXX)

add_library(
        memoryreader
        SHARED
        memory_reader.cpp)

set_target_properties(
        memoryreader
        PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON)

find_library(log-lib log)

target_link_libraries(memoryreader ${log-lib})
