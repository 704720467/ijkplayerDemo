#指定需要CMAKE的最小版本
cmake_minimum_required(VERSION 3.4.1)

file(GLOB SOURCE1 src/main/cpp/*.c )
file(GLOB SOURCE src/main/cpp/*.cpp )


add_library(
             native
             SHARED
             ${SOURCE1}
             ${SOURCE})

set(my_lib_path ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI})

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${my_lib_path} ")

include_directories(src/main/cpp/include/)

target_link_libraries(
                      native
                      avfilter avformat avcodec  avutil  swresample swscale
                      z
                      OpenSLES
                      android
                      log
                      )
