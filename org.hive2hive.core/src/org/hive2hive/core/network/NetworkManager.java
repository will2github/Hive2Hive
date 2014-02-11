package org.hive2hive.core.network;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.PublicKey;

import net.tomp2p.peers.PeerAddress;

import org.hive2hive.core.H2HSession;
import org.hive2hive.core.api.interfaces.INetworkConfiguration;
import org.hive2hive.core.exceptions.GetFailedException;
import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.core.log.H2HLogger;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.network.data.DataManager;
import org.hive2hive.core.network.data.PublicKeyManager;
import org.hive2hive.core.network.messages.MessageManager;

/**
 * The NetworkManager provides methods for establishing a connection to the
 * network, to send messages, to put and get data into the network and provides
 * all peer informations.
 * 
 * @author Seppi
 */
public class NetworkManager {

	private static final H2HLogger logger = H2HLoggerFactory.getLogger(NetworkManager.class);

	private final String nodeId;
	private final Connection connection;
	private final MessageManager messageManager;
	private final DataManager dataManager;

	private H2HSession session;
	private PublicKeyManager publicKeyManager;

	public NetworkManager(String nodeId) {
		this.nodeId = nodeId;
		this.connection = new Connection(nodeId, this);
		this.messageManager = new MessageManager(this);
		this.dataManager = new DataManager(this);
	}
	
	public String getNodeId() {
		return nodeId;
	}

	public Connection getConnection() {
		return connection;
	}

	public PeerAddress getPeerAddress() {
		return getConnection().getPeer().getPeerAddress();
	}

	/**
	 * If a user logged in, set the session in order to receive messages
	 */
	public void setSession(H2HSession session) {
		this.session = session;
	}

	/**
	 * Returns the session of the currently logged in user
	 */
	public H2HSession getSession() throws NoSessionException {
		if (session == null)
			throw new NoSessionException();
		return session;
	}

	/**
	 * Helper method that returns the public key of the currently logged in user
	 */
	public PublicKey getPublicKey() {
		try {
			return getPublicKey(getUserId());
		} catch (GetFailedException e) {
			return null;
		}
	}

	/**
	 * Helper method that returns the private key of the currently logged in user
	 */
	public PrivateKey getPrivateKey() {
		if (session == null)
			return null;
		if (publicKeyManager == null)
			publicKeyManager = new PublicKeyManager(session.getCredentials().getUserId(),
					session.getKeyPair(), dataManager);
		return publicKeyManager.getUsersPrivateKey();
	}

	/**
	 * Get the public key of the given user. The call may block.
	 * 
	 * @param userId the unique id of the user
	 * @return a public key
	 * @throws GetFailedException if a failure occurs or no public key found
	 */
	public PublicKey getPublicKey(String userId) throws GetFailedException {
		if (session == null)
			return null;
		if (publicKeyManager == null)
			publicKeyManager = new PublicKeyManager(session.getCredentials().getUserId(),
					session.getKeyPair(), dataManager);
		return publicKeyManager.getPublicKey(userId);
	}

	/**
	 * Helper method that returns the user id of the currently logged in user
	 */
	public String getUserId() {
		if (session == null)
			return null;
		return session.getCredentials().getUserId();
	}

	public void connect(INetworkConfiguration netConfig) {
		if (netConfig.isMasterPeer()) {
			connect();
		} else if (netConfig.getBootstrapPort() == -1) {
			connect(netConfig.getBootstrapAddress());
		} else {
			connect(netConfig.getBootstrapAddress(),
					netConfig.getBootstrapPort());
		}
	}

	/**
	 * Create a peer which will be the first node in the network (master).
	 * 
	 * @return <code>true</code> if creating master peer was successful, <code>false</code> if not
	 */
	private boolean connect() {
		return connection.connect();
	}

	/**
	 * Create a peer and bootstrap to a given peer through IP address
	 * 
	 * @param bootstrapInetAddress
	 *            IP address to given bootstrapping peer
	 * @return <code>true</code> if bootstrapping was successful, <code>false</code> if not
	 */
	private boolean connect(InetAddress bootstrapInetAddress) {
		return connection.connect(bootstrapInetAddress);
	}

	/**
	 * Create a peer and bootstrap to a given peer through IP address and port
	 * number
	 * 
	 * @param bootstrapInetAddress
	 *            IP address to given bootstrapping peer
	 * @param port
	 *            port number to given bootstrapping peer
	 * @return <code>true</code> if bootstrapping was successful, <code>false</code> if not
	 */
	private boolean connect(InetAddress bootstrapInetAddress, int port) {
		return connection.connect(bootstrapInetAddress, port);
	}

	/**
	 * Shutdown the connection to the p2p network.
	 */
	public void disconnect() {
		if (!connection.isConnected())
			return;
		connection.disconnect();
		if (session != null && session.getProfileManager() != null)
			session.getProfileManager().stopQueueWorker();
		logger.debug(String.format("Peer '%s' is shut down.", nodeId));
	}

	public DataManager getDataManager() throws NoPeerConnectionException {
		if (!connection.isConnected() || dataManager == null) {
			throw new NoPeerConnectionException();
		}
		return dataManager;
	}

	public MessageManager getMessageManager() throws NoPeerConnectionException {
		if (!connection.isConnected() || messageManager == null) {
			throw new NoPeerConnectionException();
		}
		return messageManager;
	}

}
