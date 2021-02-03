# CellScanner 

Android application developed to generate mobile device related location data, namely: 
* Cell information (Telephone antenna related data) 
* Location data (GPS) 


Data is stored in a local SLQLite database 
 

## Usage: 
### Starting and stopping data capture:
1. User needs to install the App (Min SDK 24) 
2. On first boot, user needs to Accept Privacy Statements 
3. Open app and start recording by switching recording state to start (to stop recording turn the switch back off) 
4. Accept data capture requests if asked

### Sending captured data to the NFI:
1. Open the App
2. Press export button and select email 


## Notes: 
* Application restarts recording after a reboot if the device was rebooted while the App was running 

 
 
## FTP test setups 
For testing with the FTP file upload  

1. Get your local ip
2. Use to following docker command to start an FTP server on your machine
```
docker run -ti -v /home/nprins/ftp:/home/vsftpd -p 20:20 -p 21:21 -p 21100-21110:21100-21110 \
-e FTP_USER=myuser -e FTP_PASS=mypass \
-e PASV_ADDRESS=<YOUR LOCALHOST IP> -e PASV_MIN_PORT=21100 -e PASV_MAX_PORT=21110 \
fauria/vsftpd
```
3. Set you local IP in the android app on the file upload

you are done 


