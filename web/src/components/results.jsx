import React from 'react';
import { Result, Modal } from 'antd';

export default function Results(props) {
  const handleCancel = () => {
    props.updateRef(false, props.resultsVisible, props.setResultsVisible);
  }

  return (
    <Modal
      title={null}
      visible={props.resultsVisible.current}
      onOk={handleCancel}
      onCancel={handleCancel}
      cancelButtonProps={{ style: { display: 'none' } }}
    >
      <Result
        icon={props.resultsObj && props.resultsObj.icon}
        title={props.resultsObj && props.resultsObj.message}
      />
    </Modal>
  )
}
