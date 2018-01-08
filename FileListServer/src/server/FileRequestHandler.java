package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import model.FileDataResponseType;
import model.FileDescriptor;
import model.FileSizeResponseType;
import model.RequestType;
import model.ResponseType;

public class FileRequestHandler extends Thread{
	private RequestType request=null;
	private DatagramPacket receivedPacket=null;
	private DatagramSocket serverSocket=null;
	private FileListServer server=null;
	
	public FileRequestHandler(FileListServer server,DatagramPacket receivedPacket, DatagramSocket serverSocket) {
		this.receivedPacket=receivedPacket;
		this.request=new RequestType(receivedPacket.getData());
        loggerManager.getInstance(this.getClass()).trace("received:"+request.toString());
		this.serverSocket=serverSocket;
		this.server=server;
	}
	
	@Override
	public void run() {
		switch(request.getRequestType()){
			case RequestType.REQUEST_TYPES.GET_FILE_LIST:
				getFileList();
				break;
			case RequestType.REQUEST_TYPES.GET_FILE_SIZE:
				getFileSize();
				break;
			case RequestType.REQUEST_TYPES.GET_FILE_DATA:
				getFileData();
				break;
			default:
				sendErrorData(ResponseType.RESPONSE_TYPES.INVALID_REQUEST_TYPE);
				
		}
		server.logTotalSentBytes();
	}
	
	private void getFileList(){
		FileDescriptor[] files=server.getFileDescriptors();
		if (files!=null){
			int numberOfFiles=files.length;
			byte[] result=new byte[0];
			for(FileDescriptor file:files){
				byte[] fileBytes=file.toByte();
				byte[] combined=new byte[result.length+fileBytes.length];
				System.arraycopy(result, 0, combined, 0, result.length);
				System.arraycopy(fileBytes, 0, combined, result.length, fileBytes.length);
				result=combined;
			}
			ResponseType response=new ResponseType(ResponseType.RESPONSE_TYPES.GET_FILE_LIST_SUCCESS, numberOfFiles, 0, 0, result);
			sendBytes(response.toByteArray());
		}
	}
	
	private void getFileSize(){
		int file_id=request.getFile_id();
		FileDescriptor[] files=server.getFileDescriptors();
		FileDescriptor foundFile=null;
		if (files!=null){
			for(FileDescriptor tmpFile:files){
				if (tmpFile.getFile_id()==file_id){
					foundFile=tmpFile;
					break;
				}
			}
		}
		if (foundFile==null){
			//File not found
			sendErrorData(ResponseType.RESPONSE_TYPES.INVALID_FILE_ID);
		}
		else{
			//get size
			File file = new File(FileListServer.FILES_FOLDER+"/"+foundFile.getFile_name());
			FileSizeResponseType response=new FileSizeResponseType(ResponseType.RESPONSE_TYPES.GET_FILE_SIZE_SUCCESS, file_id, 0, 0, file.length());
			sendBytes(response.toByteArray());
		}
	}
	
	private void getFileData(){
		int file_id=request.getFile_id();
		File file=server.getFile(file_id);
		if (file==null){
			//File not found
			sendErrorData(ResponseType.RESPONSE_TYPES.INVALID_FILE_ID);
		}
		else if (request.getStart_byte()<1 
				|| request.getEnd_byte()<request.getStart_byte() 
				|| request.getStart_byte()>file.length()){
			//start byte is less than 1
			sendErrorData(ResponseType.RESPONSE_TYPES.INVALID_START_OR_END_BYTE);
		}
		else{
			//getFileData
			long endByte=request.getEnd_byte();
			if (endByte>file.length()){
				endByte=file.length();
			}
			
			long tmpStartByte=request.getStart_byte();
			long tmpEndByte=tmpStartByte-1;
			int packageDataSize=0;
			byte[] buf=new byte[ResponseType.MAX_DATA_SIZE];
			FileDataResponseType dataResponsePackage=new FileDataResponseType(ResponseType.RESPONSE_TYPES.GET_FILE_DATA_SUCCESS,file_id,0,0,buf);
			
			RandomAccessFile raf=null;
			try {
				raf=new RandomAccessFile(file, "r");
				while(tmpEndByte<endByte){
					tmpEndByte=tmpStartByte+ResponseType.MAX_DATA_SIZE-1;
					if (tmpEndByte>endByte){
						tmpEndByte=endByte;
					}
					packageDataSize=(int)(tmpEndByte-tmpStartByte+1);
					raf.seek(tmpStartByte-1);
					raf.read(buf, 0, packageDataSize);
					dataResponsePackage.setStartByte(tmpStartByte);
					dataResponsePackage.setEndByte(tmpEndByte);
					dataResponsePackage.setData(buf);
					sendBytes(dataResponsePackage.toByteArray());
					tmpStartByte+=packageDataSize;
				}
				loggerManager.getInstance(this.getClass()).info("Transferred fileId:"+file_id+" file_name:"+file.getName()+" size:"+(endByte-request.getStart_byte()+1));
			} catch (FileNotFoundException ex) {
				loggerManager.getInstance(this.getClass()).error(ex.toString());
			} catch (IOException ex) {
				loggerManager.getInstance(this.getClass()).error(ex.toString());
			}
			finally{
				if(raf!=null){
					try {
						raf.close();
					} catch (IOException ex) {
						loggerManager.getInstance(this.getClass()).error(ex.toString());
					}
				}
			}
		}
	}
	
	private void sendErrorData(int responseType){
		ResponseType response=new ResponseType(responseType, 0, 0, 0, null);
		sendBytes(response.toByteArray());
		loggerManager.getInstance(this.getClass()).warn("Response_type:"+responseType+" Error Reponse sent to client:"+receivedPacket.getAddress().toString()+":"+receivedPacket.getPort());
	}
	
	private void sendBytes(byte[] data){
		DatagramPacket out=new DatagramPacket(data, data.length,receivedPacket.getAddress(), receivedPacket.getPort());
		try {
			serverSocket.send(out);
			server.increaseTotalSentBytes(data.length);
		} catch (IOException e) {
			loggerManager.getInstance(this.getClass()).trace(e.toString());
		}
	}
}
