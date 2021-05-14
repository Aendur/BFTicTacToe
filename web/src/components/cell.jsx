import React from 'react';
import { playerMark, playerColor } from '../constants/constants.js';

const cellStyle = {
  backgroundColor: 'gray',
  border: '0',
  height: '100px',
  width: '100px',
  textAlign: 'center',
  verticalAlign: 'top',
  textDecoration: 'none',
  display: 'inline-block',
  fontSize: '60px',
  margin: '1px',
  position: 'relative',
  whiteSpace: 'no-wrap',
}

export default function Cell(props) {
  const onClick = (value) => {
    let resp = {
      'action': 5,
      'pos': props.pos,
      'token': props.token
    }
    props.ws.send(JSON.stringify(resp));
  }

  return (
    <button style={{ ...cellStyle, color: playerColor[props.value] }} onClick={() => onClick(props.pos)}>{playerMark[props.value]}</button>
  )
}
