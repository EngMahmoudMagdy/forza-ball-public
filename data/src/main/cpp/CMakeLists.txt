cmake_minimum_required(VERSION 3.22)
project(forzasecrets)

add_library(forzasecrets SHARED secrets.cpp)

find_library(log-lib log)
target_link_libraries(forzasecrets ${log-lib})

if (DEFINED SECRETS_GEN_DIR)
    target_include_directories(forzasecrets PRIVATE ${SECRETS_GEN_DIR})
endif ()
