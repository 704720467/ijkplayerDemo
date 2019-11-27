
#include "resample.h"
#include "samplerate.h"

void resample(int channel_count, short *inputBuffer, int src_sample_rate, int dst_sample_rate,
              int input_buffer_count, short *out_buffer, int outPutBufferSize) {
    SRC_DATA src_data;
    int error = -1;
    float src_ratio = -1;
    src_ratio = (float) dst_sample_rate / src_sample_rate;
    src_data.src_ratio = src_ratio;
    //short转换为float
    float data_inPut[input_buffer_count];
    src_short_to_float_array(inputBuffer, data_inPut, input_buffer_count);
    float data_real[input_buffer_count];
    memcpy(data_real,data_inPut,input_buffer_count* sizeof(float));
    //初始化
    src_data.data_in = data_real;
    src_data.input_frames = input_buffer_count;
    float output_buffer[outPutBufferSize * channel_count];
    src_data.data_out = output_buffer;
    src_data.output_frames = outPutBufferSize;
    //重采样
    if ((error = src_simple(&src_data, SRC_LINEAR, channel_count))) {
        printf("\n\nLine %d : %s 重采样错误\n\n", __LINE__, src_strerror(error));
        exit(1);
    }
    float output_copy[outPutBufferSize * channel_count];
    memcpy(output_copy,src_data.data_out,outPutBufferSize * channel_count*sizeof(float));
    src_float_to_short_array(output_copy, out_buffer, outPutBufferSize);

}
