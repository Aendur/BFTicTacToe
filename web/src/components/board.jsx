import React, { useState, useEffect, useRef } from 'react';
import { Button, Row, Col, Input, message } from 'antd';
import Cell from './cell.jsx'
//import { FormOutlined } from '@ant-design/icons';
//import api from 'servicos/api';

const connectionStatusColor = {
  'Desconectado': 'red',
  'Conectado': 'green',
  'Conectando': 'orange'
}

export default function Board() {
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loop, setLoop] = useState(null);
  const [websocket, setWebsocket] = useState(null);
  const [connectionStatus, setConnectionStatus] = useState('Desconectado');
  const [name, setName] = useState('');
  const [gameState, setGameState] = useState({
    cells:  [0, 0, 0,
            0, 0, 0,
            0, 0, 0]
  });


  /*useEffect(() => {
    connect();
  }, []);*/

 
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

  const connect = () => {
    //const ws = new WebSocket('ws://127.0.0.1:8080');
    const protocol = 'ws:';
    const hostname = document.location.hostname;
    const port = '8080';
    const ip = protocol + '//' + hostname + ':' + port;
    var ws = new WebSocket(ip);
    setConnectionStatus('Conectando...');

    ws.onopen = () => {
      setConnectionStatus('Conectado');
      const teamRequest = {
        'action': 0,
        'name': name,
      }
      ws.send(JSON.stringify(teamRequest));
    }

    ws.onmessage = (response) => {
      response = JSON.parse(response.data);
      if (response && response.success) {
        let state = JSON.parse(response.gameState);
        setGameState({cells: state.board});
      }
      else {
        if (response && response.message) message.error(response.message);
      }
    }

    ws.onclose = () => {
      setConnectionStatus('Desconectado');
      clearInterval(loop);
    }

    setWebsocket(ws);
  }

  useInterval(() => {
    const syncRequest = {
      'action': 3,
    }
    websocket.send(JSON.stringify(syncRequest));
  }, connectionStatus === 'Conectado' ? 100 : null);

  const disconnect = () => {
    websocket.close();
  }

  const setNameValue = (e) => {
    setName(e.target.value);
  }

  return (
    <div className='main' style={{textAlign: 'center'}}>
      <div>Status: <span style={{color: connectionStatusColor[connectionStatus]}}>{connectionStatus}</span></div>
      {connectionStatus === 'Desconectado' ?
        <Row justify='center' align='middle'>
          <Col span={24}>
            Nome: <Input style={{width: '200px'}} onChange={setNameValue}></Input>
          </Col>
          <Col span={24}>
            <Button type='primary' onClick={connect} disabled={!name}>Conectar</Button>
          </Col>
        </Row>
        :
        <>
          <Button type='primary' onClick={disconnect}>Desconectar</Button>
          <Row>
            <Col span={24}>
              <Cell value={gameState.cells[0]} name={name} ws={websocket} pos={0}>X</Cell>
              <Cell value={gameState.cells[1]} name={name} ws={websocket} pos={1}>X</Cell>
              <Cell value={gameState.cells[2]} name={name} ws={websocket} pos={2}>X</Cell>
            </Col>
            <Col span={24}>
              <Cell value={gameState.cells[3]} name={name} ws={websocket} pos={3}>X</Cell>
              <Cell value={gameState.cells[4]} name={name} ws={websocket} pos={4}>X</Cell>
              <Cell value={gameState.cells[5]} name={name} ws={websocket} pos={5}>X</Cell>
            </Col>
            <Col span={24}>
              <Cell value={gameState.cells[6]} name={name} ws={websocket} pos={6}>X</Cell>
              <Cell value={gameState.cells[7]} name={name} ws={websocket} pos={7}>X</Cell>
              <Cell value={gameState.cells[8]} name={name} ws={websocket} pos={8}>X</Cell>
            </Col>
          </Row>
        </>
      }
    </div>
  )
}
