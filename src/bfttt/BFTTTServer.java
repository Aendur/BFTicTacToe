/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bfttt;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import java.io.File;
import java.io.IOException;

public class BFTTTServer extends DefaultSingleRecoverable{

	private int c = 0;
	private int id;
	private String dbPath;
	public BFTTTServer (int id) {
		this.id = id;
		this.dbPath = "serverdata" + this.id + ".dat";
		this.LoadDatabase();
		new ServiceReplica(this.id,this,this);
	}

	private void LoadDatabase() {
		File dbFile = new File(this.dbPath);
		if (dbFile.exists()) {
			// load this file
			System.out.println("Found DB file " + this.dbPath);
		} else {
			// create a new db file if it does not exist;
			System.out.println("Creating a new DB file " + this.dbPath);
			try {
				dbFile.createNewFile();
			} catch (IOException e) {
				System.out.println("Failed to create DB file " + this.dbPath);
			}
		}
	}

	@Override
	public byte[] appExecuteOrdered(byte[] bytes, MessageContext mc) {
		String request = new String(bytes);
		c++;
		System.out.println("Recebeu requisição "+c+": "+request);
		return ("Resposta "+c+" servidor: "+request).getBytes();
	}

	@Override
	public byte[] appExecuteUnordered(byte[] bytes, MessageContext mc) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public byte[] getSnapshot() {
		return Integer.toString(c).getBytes();
	}

	@Override
	public void installSnapshot(byte[] bytes) {
		c = Integer.parseInt(new String(bytes));
	}

	/**
	* @param args the command line arguments
	*/
	public static void main(String[] args) {
		// TODO code application logic here
		new BFTTTServer(Integer.parseInt(args[0]));
	}
	
}
