package com.ghbank.ifp.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializeUtils {
	
	private static Logger logger = LoggerFactory.getLogger(SerializeUtils.class);

	public static String serialize(Object obj) throws Exception {
		ByteArrayOutputStream byteArrayOutputStream = null;
		ObjectOutputStream objectOutputStream = null;

		String serStr;
		try {
			byteArrayOutputStream = new ByteArrayOutputStream();
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(obj);
			serStr = byteArrayOutputStream.toString("ISO-8859-1");
			serStr = URLEncoder.encode(serStr, "UTF-8");
		} catch (Exception e1) {
			logger.info("serialize Error:", e1);
			throw new Exception(e1);
		} finally {
			try {
				if (null != objectOutputStream) {
					objectOutputStream.close();
				}

				if (null != byteArrayOutputStream) {
					byteArrayOutputStream.close();
				}
			} catch (Exception e2) {
				logger.info("serialize Error:", e2);
			}

		}

		return serStr;
	}

	public static Object unSerialize(String serStr) throws Exception {
		if (!StringUtils.hasText(serStr)) {
			return null;
		} else {
			ByteArrayInputStream byteArrayInputStream = null;
			ObjectInputStream objectInputStream = null;

			Object obj;
			try {
				String redStr = URLDecoder.decode(serStr, "UTF-8");
				byteArrayInputStream = new ByteArrayInputStream(redStr.getBytes("ISO-8859-1"));
				objectInputStream = new ObjectInputStream(byteArrayInputStream);
				obj = objectInputStream.readObject();
			} catch (Exception e) {
				logger.info("unSerialize Error:{};", new Object[]{serStr, e});
				throw new Exception(e);
			} finally {
				try {
					if (null != objectInputStream) {
						objectInputStream.close();
					}

					if (null != byteArrayInputStream) {
						byteArrayInputStream.close();
					}
				} catch (Exception var13) {
					logger.info("unSerialize Error:", var13);
				}

			}

			return obj;
		}
	}
}