#ifndef SONIC_H_
#define SONIC_H_

#ifdef __cplusplus
extern "C" {
#endif

#define SONIC_MIN_PITCH 65
#define SONIC_MAX_PITCH 400
#define SONIC_MIN_SPEED 0.05f
#define SONIC_MAX_SPEED 20.0f

struct sonicStreamStruct;
typedef struct sonicStreamStruct* sonicStream;

sonicStream sonicCreateStream(int sampleRate, int numChannels);
void sonicDestroyStream(sonicStream stream);
int sonicWriteShortToStream(sonicStream stream, const short* samples, int numSamples);
int sonicReadShortFromStream(sonicStream stream, short* samples, int maxSamples);
int sonicFlushStream(sonicStream stream);
int sonicSamplesAvailable(sonicStream stream);
float sonicGetSpeed(sonicStream stream);
void sonicSetSpeed(sonicStream stream, float speed);
float sonicGetPitch(sonicStream stream);
void sonicSetPitch(sonicStream stream, float pitch);
float sonicGetVolume(sonicStream stream);
void sonicSetVolume(sonicStream stream, float volume);
int sonicGetSampleRate(sonicStream stream);
int sonicGetNumChannels(sonicStream stream);

#ifdef __cplusplus
}
#endif

#endif
