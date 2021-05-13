/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bfttt;

import bftsmart.tom.ServiceProxy;

public class BFTTTClient {
	public static void main(String[] args){
		ServiceProxy proxy = new ServiceProxy(1001);
		byte[] request = args[0].getBytes();
		byte[] reply = proxy.invokeOrdered(request);
		String replyString = new String(reply);
		System.out.println("Resposta recebida: "+replyString);
	}
}
