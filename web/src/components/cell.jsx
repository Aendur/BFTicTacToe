import React, { useState } from 'react';
import { Button } from 'antd';
//import { FormOutlined } from '@ant-design/icons';
//import api from 'servicos/api';

const cellStyle = {
  backgroundColor: 'gray',
  border: '0',
  color: 'blue',
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

const valueToString = {
  0: '',
  1: 'X',
  2: 'O'
}

export default function Cell(props) {
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  const onClick = (value) => {
    let resp = {
      'action': 5,
      'name': props.name,
      'pos': props.pos,
      'token': props.token
    }
    props.ws.send(JSON.stringify(resp));
  }

  return (
    <button style={cellStyle} onClick={() => onClick(props.pos)}>{valueToString[props.value]}</button>
  )
}
