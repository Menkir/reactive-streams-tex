package prototype.sync.client;

import prototype.async.client.CarConfiguration;
import prototype.model.Measurement;
import prototype.routing.RoutingFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class Car {
	private final InetSocketAddress socketAddress;
	private Socket clientSocket;
	private CarConfiguration carConfiguration;
    private RoutingFactory routingFactory = new RoutingFactory();
    private List<Measurement> route;
	/**
	 * The Flowrate define how many Requests are processed successfully.
	 */
    private int flowrate = 0;
    private boolean done = false;


	/**
	 * Initialize Car with Adress, default Car Configuration and default Route.
	 * @param socketAddress Contains the IP and Port for corresponding Server
	 */
	public Car(InetSocketAddress socketAddress) {
		this.socketAddress = socketAddress;
		carConfiguration = new CarConfiguration();
        this.route = routingFactory.getRoutingType(carConfiguration.ROUTETYPE).getRouteAsList();
	}

	/**
	 *  Initialize Car with Adress, custom Car Configuration and custom Route.
	 * @param socketAddress Contains the IP and Port for corresponding Server
	 * @param configuration The Custom Configuration for a Car Instance
	 */
    public Car(InetSocketAddress socketAddress, CarConfiguration configuration) {
        this.socketAddress = socketAddress;
        this.carConfiguration = configuration;
        this.route = routingFactory.getRoutingType(carConfiguration.ROUTETYPE).getRouteAsList();
	}

	/**
	 * Establish Connection between Car Socket and Server Socket.
	 * The socketadress as the IP and the Port for corresponding Server.
	 */
	public void connect() {
        clientSocket = new Socket();
        try {
            clientSocket.connect(socketAddress);
        } catch (IOException e) {
	        System.err.println("[Car] cannot connect Host " + e.getMessage());
        }

	}

	/**
	 *
	 * @return Integer of current Flowrate
	 */
    public int getFlowrate() {
        return flowrate;
    }

	/**
	 * This Method closes the socket to the Server.
	 */
	public void close() {
		if(clientSocket != null){
		    done = true;
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}

    /**
     * Send 100.000.000 Measurements as Simulation of infinite Emission.
     * Each Emission is delayed by a function delay() and increment a counter which controls when the Emission is done.
     * Each Measurement gets a random Singal Strength
     */
	public void send() {
        int deliveredElements = 0;
        int MAXELEMENTS = 100_000_000;
        do{
            for (Measurement measurement : route) {
	            measurement.setSignalStrength((int)(Math.random()*10));
                sendData(measurement);
                delay();
                deliveredElements ++;
            }
        }while(deliveredElements < MAXELEMENTS && !done);
	}

    /**
     * Like send() Method but with a lower defined border of Emissions.
     * @param elements number of Measurements which are send.
     */
	public void send(int elements){
        int deliveredElements = 0;
        do{
            for (Measurement measurement : route) {
	            measurement.setSignalStrength((int)(Math.random()*10));
                sendData(measurement);
                deliveredElements ++;
            }
        }while(deliveredElements < elements);
    }

    /**
     * Send Measurement via Outputstream to CarServer.
     * Receive Measurement vis InputStream.
     * Increment flowrate for analysis purpose.
     * @param measurement which is sended
     */
	private void sendData(Measurement measurement){
		try {
			// send
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
			oos.writeObject(measurement);
			oos.flush();

			// receive
			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			Measurement c = (Measurement) ois.readObject();

			// increment flowrate
             ++flowrate;
		} catch (IOException | ClassNotFoundException e) {
		    e.printStackTrace();
        }
    }

	/**
	 * Thread sleep delay from CarConfiguration
	 */
	private void delay(){
        try {
            Thread.sleep(carConfiguration.DELAY.toMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
