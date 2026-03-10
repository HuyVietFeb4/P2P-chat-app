cmake_minimum_required(VERSION 3.22.1)

project("security_native")

# Define the shared library
add_library(security_native SHARED NativeCredentials.cpp)

# Find Android's log library (useful for debugging, remove for release)
find_library(log-lib log)

# Link libraries
target_link_libraries(security_native ${log-lib})

# SECURITY: Optimization and Symbol Stripping
if(NOT DEBUG_BUILD)
    set_target_properties(security_native PROPERTIES
            C_VISIBILITY_PRESET hidden
            CXX_VISIBILITY_PRESET hidden
            VISIBILITY_INLINES_HIDDEN ON)
endif()