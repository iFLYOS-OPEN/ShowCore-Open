#ifndef __CAE_INTF_H__
#define __CAE_INTF_H__

typedef void * CAE_HANDLE; 

//angle 角度   
//channel  虚拟波束编号
//power 波束能量
//CMScore 唤醒分值
//beam 麦克风编号 
//userData 用户回调数据
typedef void (*cae_ivw_fn)(short angle, short channel, float power, short CMScore, short beam, char *param1, void *param2, void *userData);


//audioData 识别音频地址
//audioLen  识别音频字节数
//userData 用户回调数据
typedef void (*cae_audio_fn)(const void *audioData, unsigned int audioLen, int param1, const void *param2, void *userData);

//audioData 唤醒音频地址
//audioLen  唤醒音频字节数
//userData 用户回调数据
typedef void (*cae_ivw_audio_fn)(const void *audioData, unsigned int audioLen, int param1, const void *param2, void *userData);

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */


/* 初始化实例(实例地址、资源地址、唤醒信息回调、唤醒音频回调、识别音频回调、预留参数、用户回调数据) */
int CAENew(CAE_HANDLE *cae, const char* resPath, cae_ivw_fn ivwCb, cae_ivw_audio_fn ivwAudioCb, cae_audio_fn audioCb, const char *param, void *userData);
typedef int (* Proc_CAENew)(CAE_HANDLE *cae, const char* resPath, cae_ivw_fn ivwCb, cae_ivw_audio_fn ivwAudioCb, cae_audio_fn audioCb, const char *param, void *userData);

/* 重新加载资源(实例地址、资源路径) */
int CAEReloadResource(CAE_HANDLE cae, const char* resPath);
typedef int (* Proc_CAEReloadResource)(CAE_HANDLE cae, const char* resPath);

/* 写入音频数据(实例地址、录音数据地址、录音数据长度) */
int CAEAudioWrite(CAE_HANDLE cae, const void *audioData, unsigned int audioLen);
typedef int (* Proc_CAEAudioWrite)(CAE_HANDLE cae, const void *audioData, unsigned int audioLen);

/* 设置麦克风编号(唤醒模式内部已经设置编号外部不用再次调用、手动模式需要调用设置麦克风编号) */
int CAESetRealBeam(CAE_HANDLE cae, int beam);
typedef int (* Proc_CAESetRealBeam)(CAE_HANDLE cae, int beam);

/* 获取版本号 */
char* CAEGetVersion();
typedef char (* Proc_CAEGetVersion)();

/* 销毁实例(实例地址) */
int CAEDestroy(CAE_HANDLE cae);
typedef int (* Proc_CAEDestroy)(CAE_HANDLE cae);

/* 设置日志级别(日志级别 0 调试、1 信息、2错误) */
int CAESetShowLog(int show_log);
typedef int (* Proc_CAESetShowLog)(int show_log);

/* 请求鉴权(设备授权编号)*/
int CAEAuth(char *sn);
typedef int (* Proc_CAEAuth)(char *sn);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __CAE_INTF_H__ */