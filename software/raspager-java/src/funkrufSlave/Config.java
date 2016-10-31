package funkrufSlave;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class Config {

	// default
	private final String DEFAULT_NAME = "[SDRPager v1.2-SCP-#2345678]";
	private final int DEFAULT_PORT = 1337;
	private final int DEFAULT_LOGLEVEL = 0;

	// current
	private String name = DEFAULT_NAME;
	private int port = 0;
	private int logLevel = Log.NORMAL;
	private String[] master = null;

	private DataSender dataSender;

	// log
	private Log log = null;

	public Config(Log log) {
		this.log = log;
	}

	public Config(Log log, String filename) throws InvalidConfigFileException {
		this.log = log;

		if (filename != null && !filename.equals("")) {
			load(filename);
		}
	}

	public void setLog(Log log) {
		this.log = log;

		this.log.setLogLevel(this.logLevel);
	}

	private void log(String message, int type) {
		log(message, type, Log.NORMAL);
	}

	private void log(String message, int type, int logLevel) {

		if (this.log != null) {
			this.log.println(message, type, logLevel);
		}

	}

	public void load(String filename) throws InvalidConfigFileException {
		Scanner sc;

		try {
			sc = new Scanner(new File(filename));

			// check if this is a valid config file
			if (!sc.hasNextLine() || !sc.nextLine().trim().equals("#[slave config]")) {
				sc.close();
				throw new InvalidConfigFileException("Die Datei ist keine gueltige Konfigurationsdatei!");
			}

			while (sc.hasNextLine()) {
				String s = sc.nextLine();

				if (s.charAt(0) == '#') {
					log("Kommentar: " + s, Log.INFO);
					continue;
				}

				if (s.indexOf("=") < 1) {
					log("Zeile ignoriert (kein = ): " + s, Log.INFO);
					continue;
				}

				String[] p = s.split("=");

				if (p[0].equals("name")) {
					if (p.length > 1) {

						name = p[1];

					}
				} else if (p[0].equals("port")) {

					if (p.length > 1) {

						try {
							this.port = Integer.parseInt(p[1]);
						} catch (NumberFormatException e) {

							this.port = DEFAULT_PORT;

							log("Port ist auf keinen gueltigen Wert gesetzt!", Log.ERROR);
							log("Verwende Default-Port (" + this.DEFAULT_PORT + ") ...", Log.INFO);

						}

					}

				} else if (p[0].equals("master")) {
					if (p.length > 1) {
						setMaster(p[1]);
					} else {

						log("Keine Master angegeben!", Log.INFO);
					}
				} else if (p[0].equals("loglevel")) {
					if (p.length > 1) {
						try {
							this.logLevel = Integer.parseInt(p[1]);
							if (!Log.correctLogLevel(this.logLevel)) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException e) {
							// default value
							this.logLevel = this.DEFAULT_LOGLEVEL;

							log("LogLevel ist auf keinen gueltigen Wert gesetzt!", Log.ERROR);
							log("Verwende Default-LogLevel (" + DEFAULT_LOGLEVEL + ") ...", Log.INFO);
						}

						if (this.log != null) {
							this.log.setLogLevel(this.logLevel);
						}
					}
				}
			}

			sc.close();
		} catch (FileNotFoundException e) {
			log(filename + " konnte nicht gefunden/geoeffnet werden!", Log.ERROR);
			log("Verwende Default-Werte ...", Log.INFO);
			log("Diese Konfiguration filtert keine Master!", Log.INFO);
		}

		if (this.name == null) {
			this.name = this.DEFAULT_NAME;

			log("Kein Name angegeben!", Log.ERROR);
			log("Verwende Default-Name ([SDR-Pager v1.2-SCP-#2345678]) ...", Log.INFO);
		}

		if (this.port == 0) {
			this.port = this.DEFAULT_PORT;

			log("Kein Port angegeben!", Log.ERROR);
			log("Verwende Default-Port (1337) ...", Log.INFO);
		}

		log(this.toString(), Log.INFO);
	}

	public void save(String filename) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(new File(filename));

		String[] lines = { "#[slave config]", "# Port", "port=" + this.port,
				"# Erlaubte Master getrennt durch Leerzeichen", "master=" + masterToString(), "# LogLevel",
				"loglevel=" + this.logLevel };

		for (int i = 0; i < lines.length; i++) {
			writer.println(lines[i]);
		}

		writer.close();
	}

	public void loadDefault() {
		loadDefault(false);
	}

	public void loadDefault(boolean resetMaster) {
		this.name = DEFAULT_NAME;
		this.port = DEFAULT_PORT;
		this.logLevel = DEFAULT_LOGLEVEL;

		if (resetMaster) {
			this.master = null;
		}
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getLogLevel() {
		return this.logLevel;
	}

	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;

		if (this.log != null) {
			this.log.setLogLevel(logLevel);
		}
	}

	public void setDataSender(DataSender dataSender) {
		this.dataSender = dataSender;
	}

	public DataSender getDataSender() {
		return this.dataSender;
	}

	public void setMaster(String masterStr) {
		String[] p = masterStr.split(" +");

		int len = 0;

		for (int i = 0; i < p.length; i++) {

			try {

				String ip = InetAddress.getByName(p[i]).getHostAddress();

				if (i > Arrays.asList(p).indexOf(p[i])) {

					log("Doppelter Master: " + p[i], Log.INFO);
					p[i] = "";

				} else {

					p[i] = ip;
					len++;

				}

			} catch (UnknownHostException e) {

				log("Unbekannter Host: " + p[i], Log.ERROR);
				p[i] = "";

			}
		}

		this.master = new String[len];

		for (int i = 0, j = 0; i < p.length; i++) {
			if (!p[i].equals("")) {
				this.master[j] = p[i];
				log("Master eingetragen: " + this.master[j], Log.INFO);
				j++;
			}
		}

	}

	public boolean isMaster(String ip) {
		if (this.master == null) {
			return true;
		}

		return Arrays.asList(master).contains(ip);
	}

	public String masterToString() {
		if (this.master == null || this.master.length == 0)
			return "";

		String s = this.master[0];

		for (int i = 1; i < this.master.length; i++) {
			s += " " + this.master[i];
		}

		return s;
	}

	public String[] getMaster() {
		if (this.master == null) {
			return null;
		}

		String[] tmp = new String[this.master.length];

		for (int i = 0; i < this.master.length; i++) {
			tmp[i] = this.master[i];
		}

		return tmp;
	}

	@Override
	public String toString() {
		String s = "";

		s += "Konfiguration\n\n";

		s += "port=" + this.port + "\n";
		s += "master=" + masterToString() + "\n";
		s += "loglevel=" + this.logLevel + "\n";

		return s;
	}
}
