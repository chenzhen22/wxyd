package com.chenzhen.util;

import com.chenzhen.pojo.Sshbean;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;

public class SSHUtil {

	public static Session createSession(Sshbean sshBean) {
		Session sshSession = null;
		try {
			JSch jsch = new JSch();
			sshSession = jsch.getSession(sshBean.getUsername(), sshBean.getHost(), sshBean.getPort());
			sshSession.setPassword(sshBean.getPassword());
			sshSession.setConfig("userauth.gssapi-with-mic", "no");
			sshSession.setConfig("StrictHostKeyChecking", "no");
			sshSession.setConfig("kex", "diffie-hellman-group1-sha1");
			sshSession.connect();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return sshSession;
	}

	public static void upload(InputStream input, String fileName, ChannelSftp sftpChannel, String linuxPath)
			throws Exception {
		try {
			Thread.sleep(1000);
			try {
				try {
					sftpChannel.ls(linuxPath);
				}catch (Exception e2) {
					sftpChannel.mkdir(linuxPath);
				}

				sftpChannel.cd(linuxPath);
				sftpChannel.put(input, fileName);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} finally {
			Thread.sleep(1000);
			try {
				if (input != null) {
					input.close();
				}
			sftpChannel.disconnect();
			} catch (Exception e) {
				throw new Exception(e);
			}
		}
	}
	
	public static InputStream getFile(String fileName, ChannelSftp sftpChannel, String linuxPath)
			throws Exception {
		InputStream input = null;
		try {
			Thread.sleep(1000);
			try {
				sftpChannel.ls(linuxPath);
				sftpChannel.cd(linuxPath);
				input = sftpChannel.get(linuxPath + "/" + fileName);
				return input;
			} catch (Exception e1) {
				throw new Exception(e1);
			}
		} finally {
			Thread.sleep(1000);
		}
	}

	public static InputStream exec(String cmd, ChannelExec execChannel) throws Exception {
		try {
			Thread.sleep(2000);
			execChannel.setCommand(cmd);
			execChannel.setInputStream(null);
			execChannel.setErrStream(System.err); // 获取执行错误的信息
			execChannel.connect();
			return execChannel.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void close(Session sshSession, ChannelSftp sftpChannel) {
		if(null != sftpChannel) {
			sftpChannel.disconnect();
		}
		if(null != sshSession) {
			sshSession.disconnect();
		}
	}
}
