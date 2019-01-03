package com.qucetel.andrew.serialjnidemo;

/**
 * Serial Communication Management
 * the device name of first uart: /dev/ttymxc1
 * the device name of second uart: /dev/ttymxc2
 */  
public class Uart {
	/** Serial port baud rate 4800 */
	public static final int	BAUDRATE_4800 = 4800;
	/** Serial port baud rate 9600 */
	public static final int	BAUDRATE_9600 = 9600;
	/** Serial port baud rate 14400 */
	public static final int	BAUDRATE_14400 = 14400;
	/** Serial port baud rate 19200 */
	public static final int	BAUDRATE_19200 = 19200;
	/** Serial port baud rate 38400 */
	public static final int	BAUDRATE_38400 = 38400;
	/** Serial port baud rate 115200 */
	public static final int	BAUDRATE_115200 = 115200;
	
	/** uart1 device name */
	public static final String UART_DRIVER_NODE1="/dev/ttyHSL0";
	/** uart2 device name */
	public static final String UART_DRIVER_NODE2="/dev/ttyHSL1";

	/** no flow control */
	public static final int FLOWCONTROL_NONE=0;
	/** flow control XON/XOFF */
	public static final int FLOWCONTROL_XONXOFF=1;
	/** flow control RTS/CTS */
	public static final int FLOWCONTROL_RTSCTS=2;
	/** flow control XON/XOFF RTS/CTS */
	public static final int FLOWCONTROL_XONXOFF_RTSCTS=3;

	/** data bit 8 */
	public static final int DATA_BIT_8=8;
	/** data bit 7 */
	public static final int DATA_BIT_7=7;
	/** data bit 6 */
	public static final int DATA_BIT_6=6;
	/** data bit 5 */
	public static final int DATA_BIT_5=5;

	/** stop bit 1 */
	public static final int STOP_BIT_1=1;
	/** stop bit 2 */
	public static final int STOP_BIT_2=2;

	/** parity none */
	public static final int PARITY_NONE='N';
	/** parity odd */
	public static final int PARITY_ODD='O';
	/** parity even */
	public static final int PARITY_EVEN='E';

	/** NON_BLOCK */
	public static final int NON_BLOCK=0;
	/** BLOCK */
	public static final int BLOCK=1;
	
	/**
	 * the file description of opening Uart device
	 */
	private int fd=-1;
	
	/**
	 * block mode
	 */
	private int blockModel=NON_BLOCK;
	
	/**
	 * Sets the flow control mode.<br>
	 * FLOWCONTROL_NONE: no flow control<br>
	 * FLOWCONTROL_RTSCTS_IN: RTS/CTS (hardware) flow control for input<br>
	 * FLOWCONTROL_RTSCTS_OUT: RTS/CTS (hardware) flow control for output<br>
	 * FLOWCONTROL_XONXOFF_IN: XON/XOFF (software) flow control for input<br>
	 * FLOWCONTROL_XONXOFF_OUT: XON/XOFF (software) flow control for output <br>
	 * @param flowcontrol
	 * @return control Can be a bitmask combination of 
	 */
	public int setFlowControlMode(int flowcontrol){
		return setFlowControlMode_native(flowcontrol,fd);
	}
	
	/**
	 * Sets serial port parameters. 
	 * @param baudrate Serial port baud rate
	 * <br>&nbsp;BAUDRATE_4800
	 * <br>&nbsp;BAUDRATE_9600
	 * <br>&nbsp;BAUDRATE_14400
	 * <br>&nbsp;BAUDRATE_19200
	 * <br>&nbsp;BAUDRATE_38400
	 * <br>&nbsp;BAUDRATE_115200
	 * @param dataBits 
	 * <br>DATA_BIT_8
	 * <br>DATA_BIT_7
	 * <br>DATA_BIT_6
	 * <br>DATA_BIT_5
	 * @param stopBits
	 * <br>STOP_BIT_1
	 * <br>STOP_BIT_2
	 * @param parity parity
	 * <br>PARITY_NONE
	 * <br>PARITY_ODD
	 * <br>PARITY_EVEN
	 * @return int 0: success; -1: Error (The flow control mode is not supported)
	 */
	public int setSerialPortParams(int baudrate,int dataBits,int stopBits,char parity){
		return setSerialPortParams_native(baudrate,dataBits,stopBits,parity,fd);
	}
	
	/**
	 * Set the block mode.
	 * @param blockmode nonblock:0; block:1
	 * @return int 0: success; -1: Error (The block mode is not supported)
	 */
	public int setBlock(int blockmode){
		if(blockmode==0){
			blockModel=NON_BLOCK;
		}else{
			blockModel=BLOCK;
		}
	    return setBlock_native(blockmode,fd);
	}
	
	/**
	 * Are there data readable<br>
	 * if set uart mode 'NONBLOCK', reading data must be after some data reached,<br>
	 * otherwise it will return no data.<br>
	 * @param timeOut time out if no data coming.
	 * @return int 0: no data reached.
	 */
	public int select(int timeOut){
		return select_native(timeOut,fd);
	}
	
	/**
	 * open uart device
	 * Open the name of the communications port.
	 * Comment : Please do the things after openning the port. 
	 * 1)Sets the flow control mode.
	 * 2)Sets serial port parameters. 
	 * 3)Set the block mode.
	 * @param uartName device name,such as UART_DRIVER_NODE1, UART_DRIVER_NODE2
	 * @return int 0: success; -1: Error (The port open error)
	 */
	public int open(String uartName){
		fd=open_native(uartName);
		blockModel=NON_BLOCK;
		return fd>0?0:-1;
	}
	
	/**
	 * Close the communications port.
	 */
	public void close(){
		close_native(fd);
	}
	
	/**
	 * read data from port
	 * if block model is NONBLOCK, data is read by 'select' mode (Asynchronous)
	 * @param buf buffer of data
	 * @param bufsize buffer suze
	 * @return the size of data, -1 is error
	 */
	public int read(byte[] buf, int bufsize){

		return read_native(buf, bufsize,fd,blockModel);
	}


	public byte[] read_data(byte[] buf, int bufsize, int timeout){
		return read_data_native(buf,bufsize,timeout,fd);}
	/**
	 * Write the bytes to the serial.
	 * @param buf buffer of data
	 * @param writesize the size of writting.
	 * @return The size of writted data byte.-1 is error
	 */
	public int write(byte[] buf, int writesize){
		return write_native(buf,writesize,fd);
	}
	
	private native int setFlowControlMode_native(int flowcontrol,int fd);
	private native int setSerialPortParams_native(int baudrate,int dataBits,int stopBits,char parity,int fd);
	private native int setBlock_native(int blockmode,int fd);
	private native int open_native(String uartName);
	private native void close_native(int fd);
	private native int select_native(int timeOut,int fd);
	private native int read_native(byte[] buf, int bufsize,int fd,int blockModel);
	private native byte[] read_data_native(byte[] buf, int size, int timeout,int fd);
	private native int write_native(byte[] buf, int writesize,int fd);
}

