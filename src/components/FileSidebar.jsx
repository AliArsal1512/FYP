import React from 'react';
import './FileSidebar.css';

const FileSidebar = ({ isOpen, onClose, fileStructure, onFileSelect }) => {
  const findFirstJavaFile = (structure) => {
    for (const key in structure) {
      if (key === '_type') continue;
      if (structure[key]._type === 'file' && key.endsWith('.java')) {
        return structure[key];
      } else if (structure[key]._type === 'folder') {
        const result = findFirstJavaFile(structure[key]);
        if (result) return result;
      }
    }
    return null;
  };

  const buildFileTree = (structure, level = 0) => {
    const items = [];
    for (const key in structure) {
      if (key === '_type') continue;
      const item = structure[key];
      if (item._type === 'folder') {
        items.push(
          <li key={key} style={{ paddingLeft: level > 0 ? '16px' : '0' }}>
            <div className="folder">
              <span className="folder-name">📁 {key}</span>
            </div>
            <ul>{buildFileTree(item, level + 1)}</ul>
          </li>
        );
      } else if (item._type === 'file' && key.endsWith('.java')) {
        items.push(
          <li
            key={key}
            style={{ paddingLeft: level > 0 ? '16px' : '0' }}
            onClick={() => onFileSelect(item.file)}
            className="file-item"
          >
            <div className="file java">☕ {key}</div>
          </li>
        );
      }
    }
    return items;
  };

  return (
    <>
      {isOpen && <div className="sidebar-backdrop" onClick={onClose}></div>}
      <div className={`file-sidebar ${isOpen ? 'open' : ''}`}>
        <div className="sidebar-header d-flex justify-content-between align-items-center">
          <h5 className="mb-0">Explorer</h5>
          <button className="btn btn-sm btn-outline-secondary" onClick={onClose}>
            <i className="bi bi-x-lg"></i>
          </button>
        </div>
        <div className="sidebar-content p-2">
          <div className="d-flex justify-content-between align-items-center mb-2">
            <h6 className="mb-0">FILES</h6>
          </div>
          <div className="file-tree">
            {Object.keys(fileStructure).length > 0 ? (
              <ul>{buildFileTree(fileStructure)}</ul>
            ) : (
              <p className="text-muted p-2">No files uploaded</p>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default FileSidebar;

