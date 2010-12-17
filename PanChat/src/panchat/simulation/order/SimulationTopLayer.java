package panchat.simulation.order;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Queue;

import panchat.clocks.CausalMatrix;
import panchat.clocks.SavedClocks;
import panchat.clocks.VectorClock;
import panchat.data.User;
import panchat.messages.Message;
import panchat.messages.Message.Type;
import panchat.order.CausalMatrixLayer;
import panchat.order.FifoOrderLayer;
import panchat.order.OrderLayer;

public class SimulationTopLayer extends OrderLayer {

	/*
	 * Capas de ordenación
	 */
	SimulationBottomLayer bottomLayer;
	FifoOrderLayer fifo;
	CausalMatrixLayer causal;

	public SimulationTopLayer(User user, Collection<User> users) {
		this(user);

		// Debemos añadir los usuarios que pertenecerán al grupo.
		for (User u : users)
			if (!u.equals(user))
				this.addUser(u);
	}

	public SimulationTopLayer(User user) {
		super(user);

		/*
		 * Creamos las capas de orden
		 */
		bottomLayer = new SimulationBottomLayer(user);
		fifo = new FifoOrderLayer(user);
		causal = new CausalMatrixLayer(user);

		/*
		 * Vinculamos las capas en el orden inverso, ya que notifyObservers
		 * llama a los observadores en orden inverso al orden en el que son
		 * registrados.
		 */
		this.addBottomLayers(causal, fifo, bottomLayer);
		causal.addBottomLayers(fifo, bottomLayer);
		fifo.addBottomLayers(bottomLayer);
	}

	@Override
	protected boolean okayToRecv(Message msg) {
		return true;
	}

	@Override
	public Type orderCapability() {
		return null;
	}

	/**
	 * Devuelve el objeto enviado
	 * 
	 * @return Message
	 */
	public HashMap<User, Message> getSendedMsg() {
		return bottomLayer.getSendedMsg();
	}

	/**
	 * Simulamos la recepción de un mensaje. Es decir estamos simulando el
	 * momento en que el cliente recibiría a través del socket el mensaje.
	 * 
	 * @param msg
	 */
	public void receive(Message msg) {
		bottomLayer.receive(msg);
	}

	/**
	 * @return Obtenemos los mensajes que el cliente habría recibido en ese tick
	 *         tras todo el procesamiento de las capas de ordenación.
	 */
	public Collection<Message> getReceivedMsgs() {
		Queue<Message> returnQueue = deliveryQueue;
		deliveryQueue = new LinkedList<Message>();
		return returnQueue;
	}

	/**
	 * 
	 * @return Devuelve los relojes de las diferentes capas.
	 */
	public SavedClocks getClocks() {
		VectorClock sendClock = fifo.getSendClock().clone();
		VectorClock receiveClock = fifo.getReceiveClock().clone();
		CausalMatrix causalMatrix = causal.getCausalMatrix().clone();

		return new SavedClocks(sendClock, receiveClock, causalMatrix);
	}

	@Override
	public void update(Observable o, Object arg) {
		super.update(o, arg);
		if (this.deliveryQueue.size() > 0) {
			debug("\nMensaje recibido en el cliente :");
			debug("--------------------------------");
			debug(this.deliveryQueue.toString() + "\n\n");
		}
	}
}