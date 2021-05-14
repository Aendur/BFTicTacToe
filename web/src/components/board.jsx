import React from 'react';
import { Row } from 'antd';
import Cell from './cell.jsx'

export default function Board(props) {
  const { gameState, token, websocket } = props;
    return (
      <>
        <Row justify='center'>
          <Cell value={gameState.board[0]} token={token} ws={websocket.current} pos={0}></Cell>
          <Cell value={gameState.board[1]} token={token} ws={websocket.current} pos={1}></Cell>
          <Cell value={gameState.board[2]} token={token} ws={websocket.current} pos={2}></Cell>
        </Row>
        <Row justify='center'>
          <Cell value={gameState.board[3]} token={token} ws={websocket.current} pos={3}></Cell>
          <Cell value={gameState.board[4]} token={token} ws={websocket.current} pos={4}></Cell>
          <Cell value={gameState.board[5]} token={token} ws={websocket.current} pos={5}></Cell>
        </Row>
        <Row justify='center'>
          <Cell value={gameState.board[6]} token={token} ws={websocket.current} pos={6}></Cell>
          <Cell value={gameState.board[7]} token={token} ws={websocket.current} pos={7}></Cell>
          <Cell value={gameState.board[8]} token={token} ws={websocket.current} pos={8}></Cell>
        </Row>
      </>
    )
}
