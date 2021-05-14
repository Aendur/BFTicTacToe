import React, { useState, useEffect, useRef } from 'react';
import { Button, Row, Col, Input, message } from 'antd';
import Cell from './cell.jsx'
import jwt_decode from 'jwt-decode';
//import { FormOutlined } from '@ant-design/icons';
//import api from 'servicos/api';

const connectionStatusColor = {
  0: 'red',
  1: 'orange',
  2: 'green'
}

const gameStatus = {
  0: 'Aguardando oponente',
  1: 'Jogo em andamento',
  2: 'Empate',
  3: 'Vitoria Player1',
  4: 'Vitoria Player2'
}

const actions = {
  1: 'Unknown request',
  2: 'Token request',
  3: 'Join request',
  4: 'Disconnect request',
  5: 'Move request',
  6: 'Sync request'
}

const conStatus = {
  0: 'Desconectado',
  1: 'Conectando...',
  2: 'Conectado'
}

export default function Board() {
  const [connectionStatus, setConnectionStatus] = useState(0);
  const [name, setName] = useState(null);
  const [token, setToken] = useState(null);
  const [myId, setMyId] = useState(0);
  const [gameState, setGameState] = useState({
    board:  [0, 0, 0,
            0, 0, 0,
            0, 0, 0]
  });

  const websocket = useRef(null);
  var myIdRef = useRef(null);
  var tokenRef = useRef(null);
  var nameRef = useRef(null);
  var gameStateRef = useRef(null);

  useEffect(() => {
    return () => {
      if(websocket.current) websocket.current.close();
    }
  }, []);

  function useInterval(callback, delay) {
    const savedCallback = useRef();
  
    // Remember the latest callback.
    useEffect(() => {
      savedCallback.current = callback;
    }, [callback]);
  
    // Set up the interval.
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

  function updateRef(newValue, ref, stateFunc) {
    ref.current = newValue;
    stateFunc(newValue);
  }

  function handleAction(response, ws) {
    const action = response.action;
    switch(action) {
      case 2:
        if(response.token) {
          updateRef(jwt_decode(response.token).clientId, myIdRef, setMyId);
          updateRef(response.token, tokenRef, setToken);
          if(response.message) message.success(response.message);
          const request = {
            'action': 3,
            'token': response.token
          }
          ws.send(JSON.stringify(request));
        }
        else if(response.message) message.error(response.message);
        break;
      case 4:
        setConnectionStatus(0);
        setToken(null);
        setName(null);
        ws.close();
        if(response.message) message.info(response.message);
        break;
      case 6:
        if(response.gameState) {
          if(gameStateRef.current) {
            if(gameStateRef.current.status === 1 && response.gameState.status === 0) message.info('Seu oponente desconectou. O jogo foi encerrado');
          }
          updateRef(response.gameState, gameStateRef, setGameState);
        }
        break;
      default:
        break;
    }
  }

  function connect() {
    const protocol = 'ws:';
    const hostname = document.location.hostname;
    const port = '8080';
    const ip = protocol + '//' + hostname + ':' + port;
    var ws = new WebSocket(ip);
    websocket.current = ws;
    setConnectionStatus(1);

    ws.onopen = () => {
      setConnectionStatus(2);
      const request = {
        'action': 2,
        'name': name,
      }
      ws.send(JSON.stringify(request));
    }

    ws.onmessage = (response) => {
      response = JSON.parse(response.data);
      console.log(response)
      if (response && response.action) {
        handleAction(response, ws);
      }
      else if (response && response.message) message.error(response.message);
    }

    ws.onclose = () => {
      setConnectionStatus(0);
      setToken(null);
      setName(null);
      ws.close();
    }
  }

  useInterval(() => {
    const syncRequest = {
      'action': 6,
      'token': token
    }
    websocket.current.send(JSON.stringify(syncRequest));
  }, token ? 1000 : null);

  const disconnect = () => {
    const request = {
      'action': 4,
      'name': name,
      'token': token
    }
    setConnectionStatus(0);
    setToken(null);
    setName(null);
    if(websocket.current) websocket.current.send(JSON.stringify(request));
    if(websocket.current) websocket.current.close();
  }

  const setNameValue = (e) => {
    setName(e.target.value);
  }

  return (
    <div className='main' style={{textAlign: 'center'}}>
      <div>Status: <span style={{color: connectionStatusColor[connectionStatus]}}>{conStatus[connectionStatus]}</span></div>
      {connectionStatus !== 2 ?
        <Row justify='center' align='middle'>
          <Col span={24}>
            Nome: <Input style={{width: '200px'}} onChange={setNameValue} value={name} disabled={connectionStatus !== 0}></Input>
          </Col>
          <Col span={24}>
            <Button type='primary' onClick={connect} disabled={!name}>Conectar</Button>
          </Col>
        </Row>
        :
        <>
          <Button type='primary' onClick={disconnect}>Desconectar</Button>
          <Row>
            <Col span={24}>{gameStatus[gameState.status]}</Col>
            <Col span={24}>{myId}</Col>
            <Col span={24}>Player 1: {gameState.idPlayer1} VS Player2: {gameState.idPlayer2}</Col>
            <Col span={24}>
              <div>Você: {myId}</div>
              <Cell value={gameState.board[0]} myId={myId} token={token} name={name} ws={websocket.current} pos={0}>X</Cell>
              <Cell value={gameState.board[1]} myId={myId} token={token} name={name} ws={websocket.current} pos={1}>X</Cell>
              <Cell value={gameState.board[2]} myId={myId} token={token} name={name} ws={websocket.current} pos={2}>X</Cell>
            </Col>
            <Col span={24}>
              <Cell value={gameState.board[3]} myId={myId} token={token} name={name} ws={websocket.current} pos={3}>X</Cell>
              <Cell value={gameState.board[4]} myId={myId} token={token} name={name} ws={websocket.current} pos={4}>X</Cell>
              <Cell value={gameState.board[5]} myId={myId} token={token} name={name} ws={websocket.current} pos={5}>X</Cell>
            </Col>
            <Col span={24}>
              <Cell value={gameState.board[6]} myId={myId} token={token} name={name} ws={websocket.current} pos={6}>X</Cell>
              <Cell value={gameState.board[7]} myId={myId} token={token} name={name} ws={websocket.current} pos={7}>X</Cell>
              <Cell value={gameState.board[8]} myId={myId} token={token} name={name} ws={websocket.current} pos={8}>X</Cell>
            </Col>
          </Row>
        </>
      }
    </div>
  )
}
