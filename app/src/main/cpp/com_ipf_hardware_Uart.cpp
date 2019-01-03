#include "jni.h"
#include <fcntl.h>
#include <stdio.h>
#include <termios.h>
#include "ql_log.h"
#include<unistd.h>
#include <errno.h>
#include <string.h>

namespace android
{
static void throw_NullPointerException(JNIEnv *env, const char* msg)
{
    jclass clazz;
    clazz = env->FindClass("java/lang/NullPointerException");
    env->ThrowNew(clazz, msg);
}

int uartSetSerial(int speed,int databits,int stopbits, char parity,int fd){
    int i;
    int status;
    int speed_arr[] = {B115200, B38400, B19200, B9600, B4800, B2400, B1200, B300,
          B38400, B19200, B9600, B4800, B2400, B1200, B300 };
    int name_arr[] = {115200,38400,  19200,  9600,  4800,  2400,  1200,  300,      38400, 19200,  9600, 4800, 2400, 1200,  300 };

    struct termios options;
    LOGE("before get attr, fd = %d\n", fd);
    if(tcgetattr( fd,&options)!=0){
          perror("SetupSerial 1");
        LOGE("SetupSerial 1\n");
          return -1;
     }
    LOGE("in speed: %x\n", cfgetispeed(&options));
    LOGE("out speed: %x\n", cfgetospeed(&options));
    for ( i= 0; i<sizeof(speed_arr)/sizeof(int); i++){
              if  (speed == name_arr[i]){
                  LOGE("setting speed to: %d (%x)\n", name_arr[i],speed_arr[i] );
                          cfsetispeed(&options, speed_arr[i]); 
                          cfsetospeed(&options, speed_arr[i]);
              }
    }     

    options.c_cflag |= CLOCAL;

    options.c_cflag |= CREAD;

    options.c_cflag &= ~CSIZE;
    switch (databits){
       case 5    :
                     options.c_cflag |= CS5;
                     break;
       case 6    :
                     options.c_cflag |= CS6;
                     break;
       case 7    :    
                 options.c_cflag |= CS7;
                 break;
       case 8:    
                 options.c_cflag |= CS8;
                 break;
       default:
                 fprintf(stderr,"Unsupported data size/n");
                 return (-1);
    }

    switch (parity){
       case 'n':
       case 'N': //无奇偶校验位。
                 options.c_cflag &= ~PARENB;
                 options.c_iflag &= ~INPCK;
                 break; 
       case 'o':
       case 'O'://设置为奇校验    
                 options.c_cflag |= (PARODD | PARENB);
                 options.c_iflag |= INPCK;
                 break; 
       case 'e': 
       case 'E'://设置为偶校验  
                 options.c_cflag |= PARENB;
                 options.c_cflag &= ~PARODD;
                 options.c_iflag |= INPCK;       
                 break;
       case 's':
       case 'S': //设置为空格 
                 options.c_cflag &= ~PARENB;
                 options.c_cflag &= ~CSTOPB;
                 break; 
        default:  
                 fprintf(stderr,"Unsupported parity/n");   
                 return (-1);
    } 

    // 设置停止位 
    switch (stopbits){
       case 1:   
                 options.c_cflag &= ~CSTOPB; 
                 break; 
       case 2:   
                 options.c_cflag |= CSTOPB; 
                         break;
       default:   
                       fprintf(stderr,"Unsupported stop bits/n"); 
                       return (-1);
    }

    options.c_lflag  &= ~(ICANON | ECHO | ECHOE | ISIG);  /*Input*/
    options.c_oflag &= ~OPOST;


    options.c_cc[VTIME] = 1; /* 读取一个字符等待1*(1/10)s */  

    options.c_cc[VMIN] = 1; /* 读取字符的最少个数为1 */

    options.c_iflag &=~(ICRNL | IGNCR );

    //如果发生数据溢出，接收数据，但是不再读取

    tcflush(fd,TCIFLUSH);

    //激活配置 (将修改后的termios数据设置到串口中）

    if (tcsetattr(fd,TCSANOW,&options) != 0)  

    {

               perror("com set error!/n");  

       return (-1);

    }

    return (0);

}

int uartWrite(char *send_buf,int data_len,int fd){
    int len = 0;
    LOGW("send_buf: %s",send_buf);
    len = write(fd,send_buf,data_len);
    if (len!=data_len ){
        LOGW("<<<>>>>has send data %d, but not equal %d",len,data_len);
    }else{
        LOGW("send data to uart: %d, fd is %d",len,fd);
    }
    return len;
}


static void uartClose(int fd){
    close(fd);
}


static int uartOpen(char const* deviceName){
    LOGE("uartOpen()-->:deviceName = %s",deviceName);
    int fd=open(deviceName,O_RDWR|O_NONBLOCK);//读写方式
    if(fd<0){
       LOGE("uartOpen()-->:fd open failure");
       return -1;
    }

    LOGW("uartOpen()-->: open device success");
    return fd;

}

int set_mode(int nMode,int showLog, int mTtyfd){

    LOGW("set_mode:nMode%d,nshowLog=%d",nMode,showLog);
    struct termios options;
    struct termios options_read;

    if(tcgetattr(mTtyfd,&options) != 0){
       LOGE("setup serial failure");
       return -1;
    }

    if(false && showLog == 1){
           LOGI("=============read termios=============");
           LOGI("options c_cflag.CS7:%d,CS8:%d",options.c_cflag & CS7,options.c_cflag & CS8);
           LOGI("options c_cflag.PARENB:%d,PARODD:%d",options.c_cflag & PARENB,options.c_cflag & PARODD);
           LOGI("options c_iflag.INPCK%d,ISTRIP:%d",options.c_iflag & INPCK,options.c_iflag & ISTRIP);
           LOGI("option c_ispeed:%d,c_ospeed:%d",cfgetispeed(&options) ,cfgetospeed(&options));
           LOGI("options c_cflag.CSTOPB:%d,",options.c_cflag & CSTOPB);
           LOGI("options c_cc.VTIME:%d,VMIN:%d",options.c_cc[VTIME],options.c_cc[VMIN]);
           LOGI("options c_cflag.CLOCAL:%d,CREAD:%d",options.c_cflag & CLOCAL,options.c_cflag&CREAD);
           LOGI("options c_lflag.ICANON:%d,ECHO:%d,ECHOE:%d,ISIG:%d",options.c_lflag & ICANON,options.c_lflag&ECHO,options.c_lflag&ECHOE,options.c_lflag&ISIG);
           LOGI("options c_oflag.OPOST:%d,",options.c_oflag &OPOST);
           LOGI("=============read termios endi=============");
       }
       
       
    if(nMode==0){
        options.c_iflag &=~(IXON | IXOFF);
        options.c_cflag &=~(CRTSCTS);
    }else if(nMode==1){
        options.c_iflag |=(IXON | IXOFF);
        options.c_cflag &=~(CRTSCTS);
    }else if(nMode==2){
        options.c_iflag &=~(IXON | IXOFF);
        options.c_cflag |=(CRTSCTS);
    }else if(nMode==3){
        options.c_iflag |=(IXON | IXOFF);
        options.c_cflag |=(CRTSCTS);
    }

    if(tcsetattr(mTtyfd,TCSANOW,&options) != 0){
        LOGE("tcsetattr device fail");
        return -1;
    }

    if(tcgetattr(mTtyfd,&options_read) != 0){
        LOGE("setup serial failure");
        return -1;
    }

    if(false && showLog == 1){
           LOGI("=============write termios=============");
           LOGI("options_read c_cflag.CS7:%d,CS8:%d",options_read.c_cflag & CS7,options_read.c_cflag & CS8);
           LOGI("options_read c_cflag.PARENB:%d,PARODD:%d",options_read.c_cflag & PARENB,options_read.c_cflag & PARODD);
           LOGI("options_read c_iflag.INPCK%d,ISTRIP:%d",options_read.c_iflag & INPCK,options_read.c_iflag & ISTRIP);
           LOGI("options_read c_ispeed:%d,c_ospeed:%d",cfgetispeed(&options_read) ,cfgetospeed(&options_read));
           LOGI("options_read c_cflag.CSTOPB:%d,",options_read.c_cflag & CSTOPB);
           LOGI("options_read c_cc.VTIME:%d,VMIN:%d",options_read.c_cc[VTIME],options_read.c_cc[VMIN]);
           LOGI("options c_cflag.CLOCAL:%d,CREAD:%d",options_read.c_cflag & CLOCAL,options_read.c_cflag&CREAD);
           LOGI("options_read c_lflag.ICANON:%d,ECHO:%d,ECHOE:%d,ISIG:%d",options_read.c_lflag & ICANON,options_read.c_lflag&ECHO,options_read.c_lflag&ECHOE,options_read.c_lflag&ISIG);
           LOGI("options_read c_oflag.OPOST:%d,",options_read.c_oflag &OPOST);
           LOGI("=============write termios end=============");
       }

    return 0;

}

static int uart_readable(int timeout,int fd)
{
    int ret;
    fd_set set;
    struct timeval tv = { timeout / 1000, (timeout % 1000) * 1000 } ;

    FD_ZERO (&set);
    FD_SET (fd, &set);

    ret = select (fd + 1, &set, NULL, NULL, &tv);

    if (ret > 0){
        return 1;
    }

    return 0;
}

static int uart_read(char* buf, int size, int timeout,int fd)
{
    int got = 0, ret;
    do {
        ret = read (fd, buf + got, size - got);
        if (ret > 0 ) {got += ret;LOGI("got %d", got);}
        if (got >= size) break;
    }
    while (uart_readable(timeout,fd));
    return got;
}

static int uartread(char* readBuff,int buffSize,int fd,int uartBlock){
    int readCount=0;
    if(uartBlock==O_NONBLOCK){
        readCount=uart_read(readBuff,buffSize,100,fd);
    }else{
        readCount=read(fd,readBuff,buffSize);
    }
    if(readCount<0){
        LOGI("read uart data under NONBLOCK error: %d",errno);
    }else{
        LOGI("read uart data: %d; fd is %d",readCount,fd);
    }
    return readCount;
}

static int uart_select(JNIEnv *env, jobject clazz,jint timeout,jint fd){
    int ret;
    fd_set set;
    struct timeval tv = { timeout / 1000, (timeout % 1000) * 1000 } ;

    FD_ZERO (&set);
    FD_SET (fd, &set);

    ret = select (fd + 1, &set, NULL, NULL, &tv);

    if (ret > 0){
        return 1;
    }

    return 0;
}

static int open2(JNIEnv *env, jobject clazz, jstring uartName){
    const char* uartNameStr = env->GetStringUTFChars(uartName,0);
    int openInt=uartOpen(uartNameStr);
    return openInt;
}

static void close(JNIEnv *env, jobject clazz,jint fd){
    uartClose(fd);
}

static int setBlock(JNIEnv *env, jobject clazz, jint blockmode,jint fd){
    int oldflags=fcntl(fd,F_GETFL,0);
    if(oldflags==-1){
        LOGE("serial set block error");
        return -1;
    }
    if(blockmode==0){
        oldflags |= O_NONBLOCK;
    }else{
        oldflags &= ~O_NONBLOCK;
    }
    int setFlags=fcntl(fd,F_SETFL,oldflags);
    LOGI("serial set block value=%d",setFlags);
    return 0;
}

static int setSerialPortParams(JNIEnv *env, jobject clazz, jint baudrate,jint dataBits,jint stopBits,jint parity,jint fd){
    return uartSetSerial(baudrate,dataBits,stopBits,parity,fd);
}

static int setFlowControlMode(JNIEnv *env, jobject clazz, jint flowcontrol,jint fd){
	LOGI("serial set flow control: %d",flowcontrol);
    return set_mode(flowcontrol,0,fd);
}


static int read2(JNIEnv *env, jobject clazz,jbyteArray buf,jint bufSize,jint fd,jint uartBlock){
    LOGI("read data from uart to buffer[%d]",bufSize);
    jbyte* arrayData=(jbyte*)env->GetByteArrayElements(buf,0);
    int readCount=uartread((char*)arrayData,bufSize,fd,uartBlock);
    env->ReleaseByteArrayElements(buf,arrayData,0);
    return readCount;
}

static int write(JNIEnv *env, jobject clazz, jbyteArray buf, jint bufSize,jint fd){
    jbyte* arrayData = (jbyte*)env->GetByteArrayElements(buf,0);
    jsize arrayLength = env->GetArrayLength(buf);
    char* byteData = (char*)arrayData;
    int len = (int)arrayLength;
    int writeCount=uartWrite(byteData,bufSize,fd);
    env->ReleaseByteArrayElements(buf,arrayData,0);
    return writeCount;
}

static JNINativeMethod method_table[] = {
		{ "close_native", "(I)V", (void*)close },
		{ "open_native", "(Ljava/lang/String;)I", (void*)open2 },
		{ "setBlock_native", "(II)I", (void*)setBlock },
		{ "setSerialPortParams_native", "(IIIII)I", (void*)setSerialPortParams },
		{ "setFlowControlMode_native", "(II)I", (void*)setFlowControlMode },
		{ "select_native", "(II)I", (void*)uart_select},
		{ "read_native", "([BIII)I", (void*)read2 },
		{ "write_native", "([BII)I", (void*)write },
};

    extern "C"
    jint
    Java_com_qucetel_andrew_serialjnidemo_Uart_write_1native(JNIEnv *env, jobject instance,
                                                             jbyteArray buf_, jint writesize, jint fd) {
        jbyte* arrayData = (jbyte*)env->GetByteArrayElements(buf_,0);

       return uartWrite((char *) arrayData, writesize, fd);
    }

    extern "C"
    jint
    Java_com_qucetel_andrew_serialjnidemo_Uart_open_1native(JNIEnv *env, jobject instance, jstring uartName) {

        const char* s = env->GetStringUTFChars(uartName, 0);
        char item_value[128];
        strcpy(item_value, s);
        env->ReleaseStringUTFChars(uartName, s);
        LOGD("item_value = %s", item_value);
        return uartOpen(item_value);
    }

    extern "C"
    jbyteArray
    Java_com_qucetel_andrew_serialjnidemo_Uart_read_1data_1native(JNIEnv *env, jobject instance,
                                                                  jbyteArray buf_, jint bufsize,
                                                                  jint timeout, jint fd) {
        char* data = (char*)env->GetByteArrayElements(buf_, NULL);
        int num = uart_read(data, bufsize, timeout, fd);
        jbyteArray mStr = env->NewByteArray(num);
        env->SetByteArrayRegion(mStr, 0 , num, reinterpret_cast<jbyte*>(data));
        return mStr;
    }

    extern "C"
    void
    Java_com_qucetel_andrew_serialjnidemo_Uart_close_1native(JNIEnv *env, jobject instance, jint fd) {
        uartClose(fd);
    }


    extern "C"
    jint
    Java_com_qucetel_andrew_serialjnidemo_Uart_setSerialPortParams_1native(JNIEnv *env,
                                                                           jobject instance,
                                                                           jint baudrate, jint dataBits,
                                                                           jint stopBits, char parity,
                                                                           jint fd) {

        jint mint = uartSetSerial(baudrate,dataBits,stopBits,parity,fd);
        LOGD("mint = %d", mint);
        return mint;
    }


};
