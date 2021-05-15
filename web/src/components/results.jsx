import React from 'react';
import { Result, Modal } from 'antd';

export default function Results(props) {
  const handleCancel = () => {
    props.updateRef(false, props.resultsVisibleRef, props.setResultsVisible);
  }

  return (
    <Modal
      title={props.resultsObj && props.resultsObj.title}
      visible={props.resultsVisibleRef.current}
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
