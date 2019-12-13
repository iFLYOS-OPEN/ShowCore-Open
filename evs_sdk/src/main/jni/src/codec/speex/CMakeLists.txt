cmake_minimum_required(VERSION 3.4.1)

project(speex)

include_directories(include/)

aux_source_directory(src SPEEX_SRC_FILES)
set(SPEEX_SRC_FILES ${SPEEX_SRC_FILES} SpeexCodec.cpp)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2")

add_definitions(-DFLOATING_POINT -DUSE_KISS_FFT -DEXPORT= -UHAVE_CONFIG_H)
link_libraries(-llog)

add_library(speex SHARED ${SPEEX_SRC_FILES})
