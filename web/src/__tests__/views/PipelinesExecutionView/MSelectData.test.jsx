import React from 'react';
import { shallow } from 'enzyme';
import SelectDataPipelineModal from 'components/views/PipelinesExecutionView/SelectDataPipelineModal';
import { projectsArrayMock, branchesMock, filesMock } from 'testData';

const setup = ({ show, handleModalAccept }) => shallow(
  <SelectDataPipelineModal
    project={projectsArrayMock.projects.selectedProject}
    branches={branchesMock}
    show={show}
    handleModalAccept={handleModalAccept}
    selectDataClick={() => {}}
  />,
);


describe('basic rendering', () => {
  test('Assert that comp has the show class', () => {
    const wrapper = setup({ show: true, handleModalAccept: () => {} });
    wrapper.find('div#select-data-modal-div').hasClass('show');
  });
});

describe('html elements presence', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = setup({ show: true, handleModalAccept: () => {} });
    wrapper.instance().setState({ files: filesMock });
  });

  test('assert that wrapper contains and renders elements', () => {
    expect(wrapper.find('MDropdown')).toHaveLength(1);
    const selectAllBtn = wrapper.find('#select-all');
    const deselectAllBtn = wrapper.find('#deselect-all');
    const acceptBtn = wrapper.find('#accept');
    expect(selectAllBtn).toHaveLength(1);
    expect(deselectAllBtn).toHaveLength(1);
    expect(acceptBtn).toHaveLength(1);
    expect(selectAllBtn.text()).toBe('Select All');
    expect(deselectAllBtn.text()).toBe('Deselect All');
    expect(acceptBtn.text()).toBe('Accept');
    expect(wrapper.find('table#file-tree')).toHaveLength(1);
  });
});

describe('test events in files table', () => {
  let wrapper;
  const mockedAcceptBtnClick = jest.fn();
  beforeEach(() => {
    wrapper = setup({ show: true, handleModalAccept: mockedAcceptBtnClick });
    wrapper.instance().setState({ files: filesMock });
  });
  test('assert that clicking on a folder updates dom', () => {
    const mockUpdateFiles = jest.fn();
    wrapper.instance().updateFiles = mockUpdateFiles;
    wrapper.find('#button-for-0').simulate('click');
    expect(wrapper.state().filePath).toBe('directory_1');
    expect(wrapper.state().showReturnOption).toBe(true);
    const projectId = projectsArrayMock.projects.selectedProject.gid;
    const { branchSelected } = wrapper.state();
    const filePath = filesMock[0].path;
    expect(mockUpdateFiles).toHaveBeenCalledWith(projectId, filePath, branchSelected);
  });
  test('assert that handleAccept is called with the right arguments', () => {
    const fileSelected = Array.of({ ...filesMock[0], checked: true });
    const mockSelectedBranch = 'some-testing-branch';
    wrapper.setState({ files: fileSelected, branchSelected: mockSelectedBranch });
    wrapper.find('button#accept').simulate('click');
    expect(mockedAcceptBtnClick).toHaveBeenCalledWith(fileSelected, mockSelectedBranch);
  });
});
