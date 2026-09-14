cmake_minimum_required(VERSION 3.22.1)
project(pocketmp3 C)
set(LAME "${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/lame-3.100")
if(NOT EXISTS "${LAME}/configure")
 message(FATAL_ERROR "Run python3 scripts/prepare_lame.py from the project root first.")
endif()
# Autoconf generates architecture-correct headers; CMake compiles the library.
file(MAKE_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/lame-config")
if(ANDROID_ABI STREQUAL "arm64-v8a")
 set(LAME_HOST aarch64-linux-android)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
 set(LAME_HOST arm-linux-androideabi)
else()
 set(LAME_HOST x86_64-linux-android)
endif()
execute_process(COMMAND ${CMAKE_COMMAND} -E env
 "CC=${CMAKE_C_COMPILER}"
 "CFLAGS=--target=${CMAKE_C_COMPILER_TARGET} --sysroot=${CMAKE_SYSROOT} -fPIC"
 "AR=${CMAKE_AR}" "RANLIB=${CMAKE_RANLIB}"
 sh "${LAME}/configure" "--host=${LAME_HOST}"
 --disable-frontend --disable-decoder --disable-shared --disable-nasm
 WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/lame-config"
 RESULT_VARIABLE CONFIG_RESULT OUTPUT_FILE "${CMAKE_CURRENT_BINARY_DIR}/configure.log" ERROR_FILE "${CMAKE_CURRENT_BINARY_DIR}/configure-error.log")
if(NOT CONFIG_RESULT EQUAL 0)
 message(FATAL_ERROR "LAME configure failed. Inspect configure-error.log and lame-config/config.log")
endif()
file(GLOB LAME_SOURCES "${LAME}/libmp3lame/*.c" "${LAME}/libmp3lame/vector/*.c")
add_library(pocketmp3 SHARED bridge.c ${LAME_SOURCES})
target_compile_definitions(pocketmp3 PRIVATE HAVE_CONFIG_H)
target_include_directories(pocketmp3 PRIVATE "${LAME}/include" "${LAME}/libmp3lame" "${CMAKE_CURRENT_BINARY_DIR}/lame-config")
target_link_libraries(pocketmp3 m)
target_link_options(pocketmp3 PRIVATE "-Wl,-z,max-page-size=16384")
