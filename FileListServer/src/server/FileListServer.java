package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import model.FileDescriptor;
import model.InetAddressInterface;
import model.ResponseType;

public class FileListServer {
	private InetAddressInterface selectedInterface=null;
	private int port=-1;
	private FileDescriptor[] file_descriptors=null;
	private Hashtable<Integer, File> files=new Hashtable<Integer,File>();
	
	public static final String FILES_FOLDER="files";
	public static final String PROPERTIES_FILE="conf/server.properties";
	
	public FileListServer() throws SocketException{
		this.loadFileList();
		this.selectInterface();
		this.selectPort();
		this.startListening();
		this.readPropertiesFile();
	}
	
	public void readPropertiesFile(){
		Properties prop = new Properties();
		OutputStream output = null;
		int maxDataSize=1000;
		try {
			output = new FileOutputStream(PROPERTIES_FILE);
			String maxDataSizeStr=prop.getProperty("MAX_DATA_SIZE");
			maxDataSize=Integer.valueOf(maxDataSizeStr).intValue();
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
					ResponseType.MAX_DATA_SIZE=maxDataSize;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	
		}
	}

	private void loadFileList(){
		File folder = new File(FILES_FOLDER);
		File[] listOfFiles = folder.listFiles();
		
		if (listOfFiles!=null){
			ArrayList<FileDescriptor> fileArray=new ArrayList<FileDescriptor>();
			int fileNo=1;
			for(File file:listOfFiles){
				if (file.isFile()){
					FileDescriptor newFile=new FileDescriptor(fileNo, file.getName());
					fileArray.add(newFile);
					files.put(fileNo, file);
					fileNo++;
				}
			}
			this.file_descriptors = fileArray.toArray(new FileDescriptor[fileArray.size()]);
		}
	}
	
	private void selectInterface() throws SocketException{
		List<InetAddressInterface> addresses=new ArrayList<InetAddressInterface>();
		Enumeration<NetworkInterface> nets=NetworkInterface.getNetworkInterfaces();
		for(NetworkInterface net:Collections.list(nets)){
			if (net.isUp()){
				for(InetAddress inet_adr:Collections.list(net.getInetAddresses())){
					if (inet_adr instanceof Inet4Address){
						InetAddressInterface adr=new InetAddressInterface(net, inet_adr);
						addresses.add(adr);
					}
				}
			}
		}
		
		if (!addresses.isEmpty()){
			for(int i=0;i<addresses.size();i++){
				InetAddressInterface adr=addresses.get(i);
				System.out.println("["+(i+1)+"] "+adr.toString());
			}
			selectedInterface=null;
			while(selectedInterface==null){
				System.out.print("Please select interface by entering the number in []:");
				Scanner in = new Scanner(System.in);
				try{
					int selectedIndex=Integer.valueOf(in.next()).intValue();
					if (selectedIndex>0 && selectedIndex<=addresses.size()){
						selectedInterface=addresses.get(selectedIndex-1);
					}
				} catch(Exception ex){}
			}
		}
		loggerManager.getInstance(this.getClass()).debug("selectedInterface: "+selectedInterface.getNetqorkInterface().getName()
				+selectedInterface.getInetAddress().toString());
	}
	
	private void selectPort(){
		System.out.print("Please enter port number:");
		Scanner in = new Scanner(System.in);
		try{
			this.port=Integer.valueOf(in.next()).intValue();
		} catch(Exception ex){}
		loggerManager.getInstance(this.getClass()).debug("selectedPort: "+port);
	}
	
	private void startListening() throws SocketException{
		if (selectedInterface!=null && port>0){
			DatagramSocket serverSocket=new DatagramSocket(port, selectedInterface.getInetAddress());
			byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE()];
			 while(true){
				 try{
					 DatagramPacket receivePacket=new DatagramPacket(receiveData, receiveData.length);
		             serverSocket.receive(receivePacket);
		             FileRequestHandler reqHandler=new FileRequestHandler(this,receivePacket, serverSocket);
		             reqHandler.start();
				 }catch(IOException ex){
					 loggerManager.getInstance(this.getClass()).error(ex.toString());
				 }
		      }
		}
	}
	
	public FileDescriptor[] getFileDescriptors(){
		return file_descriptors;
	}
	
	public File getFile(int file_id){
		return files.get(file_id);
	}
	
	public static void main(String[] args) throws SocketException{
		FileListServer inst=new FileListServer();
	}
}
