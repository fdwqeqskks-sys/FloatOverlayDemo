#include <jni.h>

#include <cstdint>
#include <cstdio>
#include <cstring>
#include <limits>

namespace {

float sample_value = 123.456F;

void throw_illegal_argument(JNIEnv *env, const char *message) {
    jclass exception_class = env->FindClass("java/lang/IllegalArgumentException");
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message);
    }
}

bool is_readable_range(uintptr_t address, size_t byte_count) {
    if (address == 0 || byte_count == 0
            || address > std::numeric_limits<uintptr_t>::max() - byte_count) {
        return false;
    }

    FILE *maps = std::fopen("/proc/self/maps", "r");
    if (maps == nullptr) {
        return false;
    }

    const uintptr_t requested_end = address + byte_count;
    char line[512] = {};
    bool readable = false;

    while (std::fgets(line, sizeof(line), maps) != nullptr) {
        unsigned long long region_start = 0;
        unsigned long long region_end = 0;
        char permissions[5] = {};

        if (std::sscanf(line, "%llx-%llx %4s",
                        &region_start, &region_end, permissions) != 3) {
            continue;
        }

        if (permissions[0] == 'r'
                && address >= static_cast<uintptr_t>(region_start)
                && requested_end <= static_cast<uintptr_t>(region_end)) {
            readable = true;
            break;
        }
    }

    std::fclose(maps);
    return readable;
}

}  // namespace

extern "C" JNIEXPORT jfloat JNICALL
Java_com_example_floatoverlay_MemoryReader_readFloat(
        JNIEnv *env, jclass, jlong raw_address) {
    if (raw_address <= 0) {
        throw_illegal_argument(env, "Address must be greater than zero");
        return 0.0F;
    }

    const auto address = static_cast<uintptr_t>(raw_address);
    if (!is_readable_range(address, sizeof(float))) {
        throw_illegal_argument(env, "Address is not readable in this app process");
        return 0.0F;
    }

    float value = 0.0F;
    std::memcpy(&value, reinterpret_cast<const void *>(address), sizeof(value));
    return value;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_floatoverlay_MemoryReader_createSample(
        JNIEnv *, jclass, jfloat value) {
    sample_value = value;
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(&sample_value));
}
