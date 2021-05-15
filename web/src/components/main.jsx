import React, { useState, useEffect, useRef } from 'react';
import { Button, Row, Col, Input, message, Card, Spin, Typography, Divider } from 'antd';
import jwt_decode from 'jwt-decode';
import Board from './board.jsx';
import { connectionStatusColor, gameStatus, conStatus, playerColor, playerMark, syncDelay, socketPort } from '../constants/constants.js';

export default function Main() {
  const [connectionStatus, setConnectionStatus] = useState(0);
  const [name, setName] = useState(null);
  const [token, setToken] = useState(null);
  const [myId, setMyId] = useState(0);
  const [myPlayer, setMyPlayer] = useState(0);
  const [gameState, setGameState] = useState({
    board: [0, 0, 0,
      0, 0, 0,
      0, 0, 0]
  });

  const websocket = useRef(null);
  var connectionStatusRef = useRef(null);
  var nameRef = useRef(null);
  var tokenRef = useRef(null);
  var myIdRef = useRef(null);
  var myPlayerRef = useRef(null);
  var gameStateRef = useRef(null);

  useEffect(() => {
    return () => {
      if (websocket.current && websocket.current.readyState === WebSocket.OPEN) disconnect();
    }
  }, []);

  const resetState = () => {
    updateRef(0, connectionStatusRef, setConnectionStatus);
    updateRef(null, tokenRef, setToken);
    updateRef(null, nameRef, setName);
    updateRef(null, myIdRef, setMyId);
    updateRef(null, myPlayerRef, setMyPlayer);
    updateRef({ board: [0, 0, 0, 0, 0, 0, 0, 0, 0] }, gameStateRef, setGameState);
  }

  const useInterval = (callback, delay) => {
    const savedCallback = useRef();
    useEffect(() => {
      savedCallback.current = callback;
    }, [callback]);
    useEffect(() => {
      function tick() {
        savedCallback.current();
      }
      if (delay !== null) {
        let id = setInterval(tick, delay);
        return () => clearInterval(id);
      }
    }, [delay]);
  }

  const updateRef = (newValue, ref, stateFunc) => {
    ref.current = newValue;
    stateFunc(newValue);
  }

  const findMyPlayer = (state) => {
    if (myId === state.idPlayer1 || myIdRef.current === state.idPlayer1) return 1;
    if (myId === state.idPlayer2 || myIdRef.current === state.idPlayer2) return 2;
    return null;
  }

  const handleAction = (response, ws) => {
    const action = response.action;
    switch (action) {
      case 2:
        if (response.token) {
          updateRef(jwt_decode(response.token).clientId, myIdRef, setMyId);
          updateRef(response.token, tokenRef, setToken);
          if (response.message) message.success(response.message);
          const request = {
            'action': 3,
            'token': response.token
          }
          ws.send(JSON.stringify(request));
        }
        else if (response.message) message.error(response.message);
        break;
      case 3:
        message.success('Você foi adicionado ao jogo');
        break;
      case 4:
        resetState();
        disconnect();
        if (response.message) message.info(response.message);
        break;
      case 5:
        if (response.message) message.info(response.message);
        break;
      case 6:
        if (response.gameState) {
          if (gameStateRef.current) {
            if (gameStateRef.current.status === 1 && response.gameState.status === 0) message.info('Seu oponente desconectou. O jogo foi encerrado');
          }
          if (!myPlayer) {
            updateRef(findMyPlayer(response.gameState), myPlayerRef, setMyPlayer);
          }
          updateRef(response.gameState, gameStateRef, setGameState);
        }
        break;
      default:
        break;
    }
  }

  const connect = () => {
    const protocol = 'ws:';
    const hostname = document.location.hostname;
    const port = socketPort;
    const ip = protocol + '//' + hostname + ':' + port;
    var ws = new WebSocket(ip);
    websocket.current = ws;
    updateRef(1, connectionStatusRef, setConnectionStatus);

    ws.onopen = () => {
      updateRef(2, connectionStatusRef, setConnectionStatus);
      const request = {
        'action': 2,
        'name': name
      }
      ws.send(JSON.stringify(request));
    }

    ws.onmessage = (response) => {
      response = JSON.parse(response.data);
      console.log(response);
      if (response && response.action) {
        handleAction(response, ws);
      }
      else if (response && response.message) message.error(response.message);
    }

    ws.onclose = () => {
      message.error('A conexão com o servidor foi perdida');
      resetState();
    }
  }

  useInterval(() => {
    const syncRequest = {
      'action': 6,
      'token': token
    }
    websocket.current.send(JSON.stringify(syncRequest));
  }, (token && websocket.current.readyState === WebSocket.OPEN) ? syncDelay : null);

  const disconnect = () => {
    const request = {
      'action': 4,
      'name': name,
      'token': token
    }
    resetState();
    if (websocket.current && websocket.current.readyState === WebSocket.OPEN){
      websocket.current.send(JSON.stringify(request));
      websocket.current.close();
    }
  }

  const setNameValue = (e) => {
    updateRef(e.target.value, nameRef, setName);
  }

  return (
    <div className='main' style={{ textAlign: 'center' }}>
      {connectionStatus !== 2 ?
        <Row justify='center'>
          <Col span={24}>
            Nome: <Input style={{ width: '200px' }} onChange={setNameValue} value={name} disabled={connectionStatus !== 0}></Input>
          </Col>
          <Col span={24}>
            <Button type='primary' onClick={connect} disabled={!name}>Conectar</Button>
          </Col>
          <Divider />
          <Typography.Title level={4} style={{ color: connectionStatusColor[connectionStatus] }}>Status da conexão: {conStatus[connectionStatus]}</Typography.Title>
        </Row>
        :
        <>
          <Button type='primary' onClick={disconnect} style={{ marginBottom: '1em' }}>Desconectar</Button>
          <Divider />
          <Row justify='center'>
            <Col>
              <Typography.Title level={4} style={{ color: connectionStatusColor[connectionStatus] }}>Status da conexão: {conStatus[connectionStatus]}</Typography.Title>
              <Typography.Title level={4}>Status da partida: {gameStatus[gameState.status]}</Typography.Title>
            </Col>
          </Row>
          <Row justify='center' align='middle'>
            <Col span={8}>
              <Row justify='center'>
                <Card size='small' style={{ width: 200 }} headStyle={{ color: playerColor[1] }} title={gameState.status === 0 ? '-' : gameState.namePlayer1}>
                  <Spin tip='Aguardando oponente' spinning={gameState.status === 0 && myPlayer !== 1}>
                    {(gameState.status !== 0 || myPlayer === 1) &&
                      <div>
                        <p style={{ fontWeight: 700 }}>{gameState.turn === 1 ? 'Sua vez!' : null}</p>
                        <p style={{ fontSize: '60px', color: playerColor[1] }}>{playerMark[1]}</p>
                      </div>}
                  </Spin>
                </Card>
              </Row>
            </Col>
            <Col span={8}>
              <Board gameState={gameState} token={token} websocket={websocket}></Board>
            </Col>
            <Col span={8}>
              <Row justify='center'>
                <Card size='small' style={{ width: 200 }} headStyle={{ color: playerColor[2] }} title={gameState.status === 0 ? '-' : gameState.namePlayer2}>
                  <Spin tip='Aguardando oponente' spinning={gameState.status === 0 && myPlayer !== 2}>
                    {(gameState.status !== 0 || myPlayer === 2) &&
                      <div>
                        <p style={{ fontWeight: 700 }}>{gameState.turn === 2 ? 'Sua vez!' : null}</p>
                        <p style={{ fontSize: '60px', color: playerColor[2] }}>{playerMark[2]}</p>
                      </div>}
                  </Spin>
                </Card>
              </Row>
            </Col>
          </Row>
        </>
      }
    </div>
  )
}
