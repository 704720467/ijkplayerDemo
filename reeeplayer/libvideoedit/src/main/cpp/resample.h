#include "config.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <assert.h>

#include "samplerate.h"

void resample(int channel_count, short *inputBuffer, int src_sample_rate, int dst_sample_rate, int input_buffer_count, short *out_buffer, int outPutBufferSize);