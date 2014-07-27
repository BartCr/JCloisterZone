package com.jcloisterzone.rmi.mina;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcloisterzone.Application;
import com.jcloisterzone.event.setup.PlayerSlotChangeEvent;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.PlayerSlot;
import com.jcloisterzone.game.phase.CreateGamePhase;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.rmi.ServerIF;
import com.jcloisterzone.wsio.CmdHandler;
import com.jcloisterzone.wsio.Connection;
import com.jcloisterzone.wsio.message.GameMessage;
import com.jcloisterzone.wsio.message.JoinGameMessage;
import com.jcloisterzone.wsio.message.SlotMessage;
import com.jcloisterzone.wsio.message.WelcomeMessage;
import com.jcloisterzone.wsio.server.SimpleServer;


public abstract class ClientStub  implements InvocationHandler {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private Connection conn;
    private ServerIF serverProxy;

    protected Game game;


    public Connection  connect(InetAddress ia, int port) throws URISyntaxException {
        return connect(null);
    }

    private Connection connect(SocketAddress endpoint) throws URISyntaxException {
        ////localhost:8000/ws")) {
        conn = new Connection(new URI("ws://localhost:37447/"), this);
        return conn;
    }


    public ServerIF getServerProxy() {
        return serverProxy;
    }

    public void setServerProxy(ServerIF serverProxy) {
        this.serverProxy = serverProxy;
    }

    public Game getGame() {
        return game;
    }

    //TODO revise; close from client side ???
    public void stop() {
        conn.close();
        conn = null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (conn == null) {
            logger.info("Not connected. Message ignored");
        } else {
            //session.write(new CallMessage(method, args));
        }
        return null;
    }

//    @Override
//    public final void messageReceived(IoSession session, Object message) {
//        if (message instanceof ControllMessage) {
//            ControllMessage cm = (ControllMessage) message;
//            clientId = cm.getClientId();
//            if (cm.getProtocolVersion() != Application.PROTCOL_VERSION) {
//                versionMismatch(cm.getProtocolVersion());
//                session.close(true);
//                return;
//            }
//            controllMessageReceived(cm);
//        } else {
//            callMessageReceived((CallMessage) message);
//        }
//    }

    protected void versionMismatch(int version) {
        logger.error("Version mismatch. Server version: " + version +". Client version " + Application.PROTCOL_VERSION);
    }

    protected Game createGame(GameMessage message) {
        if (message.getSnapshot() == null) {
            game = new Game();
        } else {
            throw new UnsupportedOperationException("not implemented");
            //game = msg.getSnapshot().asGame();
        }
        game = new Game();
        return game;
    }

    @CmdHandler("WELCOME")
    public void handleWelcomeMessage(Connection conn, WelcomeMessage msg) {
        //conn.sendMessage("CREATE_GAME", new CreateGameMessage());
        conn.sendMessage("JOIN_GAME", new JoinGameMessage(SimpleServer.GAME_ID));
    }

    private void updateSlot(PlayerSlot[] slots, SlotMessage slotMsg) {
        PlayerSlot slot = slots[slotMsg.getNumber()];
        slot.setNickname(slotMsg.getNickname());
        slot.setState(slotMsg.getState());
        slot.setSerial(slotMsg.getSerial());
        if (!slot.isOwn() || !slotMsg.isAi()) {
            slot.setAiClassName(null);
        }
    }

    @CmdHandler("GAME")
    public void handleGameMessage(Connection conn, GameMessage msg) {
        game = createGame(msg);
        CreateGamePhase phase;
        if (msg.getSnapshot() == null) {
            phase = new CreateGamePhase(game, getServerProxy());
        } else {
            throw new UnsupportedOperationException("not implemented");
            //phase = new LoadGamePhase(game, msg.getSnapshot(), getServerProxy());
        }
        // TODO - lagacy bridge
        PlayerSlot[] slots = new PlayerSlot[PlayerSlot.COUNT];
        for (int i = 0; i < slots.length; i++) {
            PlayerSlot slot = new PlayerSlot(i);
            slot.setColors(game.getConfig().getPlayerColor(slot));
            slots[i] = slot;
        }
        for (SlotMessage slotMsg : msg.getSlots()) {
            updateSlot(slots, slotMsg);
        }
        phase.setSlots(slots);
        game.getPhases().put(phase.getClass(), phase);
        game.setPhase(phase);
    }

    @CmdHandler("SLOT")
    public void handleSlotMessage(Connection conn, SlotMessage msg) {
        final PlayerSlot[] slots = ((CreateGamePhase) game.getPhase()).getPlayerSlots();
        updateSlot(slots, msg);
        game.post(new PlayerSlotChangeEvent(slots[msg.getNumber()]));
    }


//    protected void callMessageReceived(CallMessage msg) {
//        try {
//            Phase phase = game.getPhase();
//            logger.debug("Delegating {} on phase {}", msg.getMethod(), phase.getClass().getSimpleName());
//            msg.call(phase, ClientIF.class);
//            phase = game.getPhase(); //new phase can differ from the phase in prev msg.call !!!
//            while (phase != null && !phase.isEntered()) {
//                logger.debug("Entering phase {}",  phase.getClass().getSimpleName());
//                phase.setEntered(true);
//                phase.enter();
//                phase = game.getPhase();
//                //game.post(new PhaseEnterEvent(phase));
//            }
//        } catch (InvocationTargetException ie) {
//            logger.error(ie.getMessage(), ie.getCause());
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//        }
//    }


    public String getClientId() {
        return conn.getClientId();
    }

//    @Override
//    public void exceptionCaught(IoSession brokenSession, Throwable cause) {
//        //temporary disabled, not worked as intended
////        SocketAddress endpoint = brokenSession.getServiceAddress();
////        session = null;
////        int delay = 500;
////        logger.warn("Connection lost. Reconnecting to " + endpoint + " ...");
////        onDisconnect();
////        while (session == null) {
////
////            try {
////                Thread.sleep(delay);
////            } catch (InterruptedException e) {
////            }
////            connect(endpoint);
////            if (delay < 4000) delay *= 2;
////        }
////        onReconnect();
////        session.write(new ClientControllMessage(clientId));
//    }
//
//    protected void onDisconnect() {
//    }
//
//    protected void onReconnect() {
//    }

}
