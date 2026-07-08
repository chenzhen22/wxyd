package com.chenzhen.test;

import cfca.yuzhi.fep.toolkit.ClientContext;
import cfca.yuzhi.vo.request.CertServiceRequestTx11VO;
import cfca.yuzhi.vo.request.CertServiceRequestTx12VO;
import cfca.yuzhi.vo.request.CertServiceRequestTx13VO;
import cfca.yuzhi.vo.request.MakeCertRequestVO;
import cfca.yuzhi.vo.response.CertServiceResponseTx11VO;
import cfca.yuzhi.vo.response.CertServiceResponseTx12VO;
import cfca.yuzhi.vo.response.CertServiceResponseTx13VO;
import cfca.yuzhi.vo.response.MakeCertResponseVO;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CFCATEST
{
  private static Scanner scan = new Scanner(System.in);
  
  public static void main(String[] args)
  {
    String socketServerIP = "10.1.87.178";
    int socketServerPort = 9043;
    int connectTimeout = 2000;
    int readTimeout = 4000;
    ClientContext.initSocket(socketServerIP, socketServerPort, connectTimeout, readTimeout);
    //query();
    //unbind();
    bind();
    //huanfa();
    //revoke();
    //replace();

  }

  private static void replace() {
    CertServiceRequestTx11VO request = new CertServiceRequestTx11VO();
    request.setKeyID("830802A100001703");//KeyID
    request.setUserName("237358");//用户名
    request.setIdentificationType("Z");
    request.setIdentificationNo("430726199401290015");//证件号码
    request.setRevokeKeyID("830803B100000422");//KeyID（必须16位）被吊销的证书的KeyId
    CertServiceResponseTx11VO response = ClientContext.getInstance().tx1102(request);
    System.out.println(response.getCode());
    System.out.println(response.getMessage());
    System.out.println(response.getCertType());
    System.out.println(response.getSubjectDn());
    System.out.println(response.getCertStatus());
    System.out.println(response.getSerialNo());
    System.out.println(response.getNotAfter());
    System.out.println(response.getNotBefore());
    System.out.println(response.getKeyID());
    System.out.println(response.getKeyAlg());
    System.out.println(response.getKeyLength());
  }

  private static void revoke() {
    CertServiceRequestTx12VO request = new CertServiceRequestTx12VO();
    request.setKeyID(CheckKeyId());
    CertServiceResponseTx12VO response = ClientContext.getInstance().tx1206(request);
    System.out.println(response.getCode());
    System.out.println(response.getMessage());
    System.out.println(response.getCertType());
    System.out.println(response.getSubjectDn());
    System.out.println(response.getCertStatus());
    System.out.println(response.getSerialNo());
    System.out.println(response.getNotAfter());
    System.out.println(response.getNotBefore());
    System.out.println(response.getKeyID());
    System.out.println(response.getKeyAlg());
    System.out.println(response.getKeyLength());
  }

  /**
   * 证书下载
   */
  private static void sign()
  {
    MakeCertRequestVO request = new MakeCertRequestVO();
    request.setSerialNo("2036940075");
    request.setAuthCode("34CQXCQFMR");
    request.setP10("MIIBHDCBwAIBADBgMQswCQYDVQQGEwJDTjEOMAwGA1UECgwFT0NBMTExDzANBgNVBAsMBlRQQy1TMzEVMBMGA1UECwwMSW5kaXZpZHVhbC0xMRkwFwYDVQQDDBA4MzA4MDNCMTAwMDAwNjM2MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEVYRek4jDmI9oYjx+PsJjmyIs3h7Eg4tdevc+oNcIvvOAI5iE97w/tD9okBja/rVYclJwjvzctcPboDqBlILvpjAMBggqgRzPVQGDdQUAA0kAMEYCIQD9J05WSGDvE7NkaW5zl+c6X9kTBu6LRPo0kIfH9/DtbAIhAJYNiaiC7aiM0bhS0YWhSKM8aG4qGRPs9BSWwujdNVZ+");
    MakeCertResponseVO response = ClientContext.getInstance().tx1208(request);
    System.out.println(response.getCode());
    System.out.println(response.getMessage());
    System.out.println(response.getCertType());
    System.out.println(response.getSubjectDn());
    System.out.println(response.getCertStatus());
    System.out.println(response.getSerialNo());
    System.out.println(response.getNotAfter());
    System.out.println(response.getNotBefore());
    System.out.println(response.getKeyID());
    System.out.println(response.getKeyAlg());
    System.out.println(response.getKeyLength());
  }
  
  /**
   * 证书绑定
   */
  private static void bind()
  {
    CertServiceRequestTx11VO request = new CertServiceRequestTx11VO();
    request.setKeyID(CheckKeyId());
    request.setUserName("101625");
    request.setIdentificationType("Z");
    request.setIdentificationNo("512828199207021769");
    CertServiceResponseTx11VO response = ClientContext.getInstance().tx1101(request);
    String code = response.getCode();
    if (code.equals("2000"))
    {
      System.out.println(response.getCode());
      System.out.println(response.getMessage());
      System.out.println(response.getCertType());
      System.out.println(response.getSubjectDn());
      System.out.println(response.getCertStatus());
      System.out.println(response.getSerialNo());
      System.out.println(response.getNotAfter());
      System.out.println(response.getNotBefore());
      System.out.println(response.getKeyID());
      System.out.println(response.getKeyAlg());
      System.out.println(response.getKeyLength());
    }
    else
    {
      System.out.println(response.toString());
      System.out.println(response.getCode());
      System.out.println(response.getMessage());
    }
  }
  
  /**
   * 证书解绑
   */
  private static void unbind()
  {
    CertServiceRequestTx11VO request = new CertServiceRequestTx11VO();
    request.setKeyID(CheckKeyId());
    CertServiceResponseTx11VO response = ClientContext.getInstance().tx1103(request);
    System.out.println(response.toString());
  }
  
  /**
   * 证书解冻
   */
  private static void active()
  {
    CertServiceRequestTx12VO request = new CertServiceRequestTx12VO();
    request.setKeyID(CheckKeyId());
    CertServiceResponseTx12VO response = ClientContext.getInstance().tx1202(request);
    String code = response.getCode();
    if (code.equals("2000"))
    {
      System.out.println(response.getCertType());
      System.out.println(response.getSubjectDn());
      System.out.println(response.getCertStatus());
      System.out.println(response.getSerialNo());
      System.out.println(response.getNotAfter());
      System.out.println(response.getNotBefore());
      System.out.println(response.getKeyID());
      System.out.println(response.getKeyAlg());
      System.out.println(response.getKeyLength());
    }
    else
    {
      System.out.println(response.getCode());
      System.out.println(response.getMessage());
    }
  }
  
  /**
   * 证书查询
   */
  private static void query()
  {
    CertServiceRequestTx13VO request = new CertServiceRequestTx13VO();
    request.setKeyID(CheckKeyId());
    CertServiceResponseTx13VO response = ClientContext.getInstance().tx1301(request);
    System.out.println(response.toString());
    System.out.println("错误码：" + response.getCode());
    System.out.println("错误信息：" + response.getMessage());
    System.out.println("key编号：" + response.getKeyID());
    System.out.println("SN：" + response.getSubjectDn());
    System.out.println("DN：" + response.getIssueDn());
    System.out.println("用户名：" + response.getUserName());
    System.out.println("证件类型：" + response.getIdentificationType());
    System.out.println("证件号码：" + response.getIdentificationNo());
    System.out.println("生效日：" + response.getStartTime());
    System.out.println("截止日：" + response.getEndTime());
    System.out.println("证书状态：" + response.getCertStatus());
    System.out.println("证书序列号：" + response.getSerialNo());
  }
  
  /**
   * 证书换发（更新）
   */
  private static void huanfa()
  {
	CertServiceRequestTx12VO request = new CertServiceRequestTx12VO();
    request.setKeyID(CheckKeyId());
    CertServiceResponseTx12VO response = ClientContext.getInstance().tx1204(request);
    System.out.println(response.toString());
    System.out.println("错误码：" + response.getCode());
    System.out.println("错误信息：" + response.getMessage());
    System.out.println("key编号：" + response.getKeyID());
    System.out.println("SN：" + response.getSubjectDn());
    System.out.println("DN：" + response.getIssueDn());
    System.out.println("证书状态：" + response.getCertStatus());
    System.out.println("证书序列号：" + response.getSerialNo());
    System.out.println("证书授权码：" + response.getAuthCode());
  }
  
  public static String CheckKeyId()
  {
    String keyId = "";
    String reg = "(^[0-9A-Za-z]+$)";
    int index = 1;
    do
    {
      if (index == 1)
      {
        System.out.println("请输入key编号(exit:退出)：");
        keyId = scan.nextLine();
        if ("exit".equals(keyId)) {
          System.exit(0);
        }
      }
      else
      {
        System.out.println("格式不对，请重新输入key编号(exit:退出)：");
        keyId = scan.nextLine();
        if ("exit".equals(keyId)) {
          System.exit(0);
        }
      }
      index++;
    } while (!Pattern.compile(reg).matcher(keyId).matches());
    return keyId;
  }
}
