import React, { Component } from 'react';
import { connect } from 'react-redux';
import {
  number,
  string,
  arrayOf,
  shape,
  bool,
} from 'prop-types';
import { Link } from 'react-router-dom';
import MDropdown from 'components/ui/MDropdown';
import { getFileDifferences } from 'functions/apiCalls';
import { getTimeCreatedAgo, getCommentFromCommit } from 'functions/dataParserHelpers';
import Navbar from '../navbar/navbar';
import ProjectContainer from '../projectContainer';
import './commitDetails.css';
import CommitsApi from '../../apis/CommitsApi';
import ImageDiffSection from '../imageDiffSection/imageDiffSection';

const imageFormats = [
  '.png',
  '.jpg',
];

const commitsApi = new CommitsApi();

class CommitDetails extends Component {
  constructor(props) {
    super(props);
    const { users } = this.props;
    this.state = {
      commits: {},
      users,
      imagesToRender: [],
      files: [],
    };
    const { match } = this.props;
    const { projectId, commitId } = match.params;
    commitsApi.getCommitDetails(projectId, commitId)
      .then((response) => {
        this.setState({ commits: response });
        CommitsApi
          .getCommitDiff(projectId, commitId)
          .then((res) => {
            this.setState({ files: res });
            return this.getDiffDetails(res);
          })
          .catch(
            (err) => err,
          );
      })
      .catch((err) => err);
  }

  getDiffDetails(diffsArray) {
    const { commits } = this.state;
    const { match } = this.props;
    const { projectId } = match.params;
    diffsArray.filter((diff) => imageFormats
      .filter((format) => diff.old_path.includes(format))
      .length > 0)
      .forEach(async (imageDiff) => {
        const { imagesToRender } = this.state;
        const {
          previousVersionFile,
          nextVersionFile,
        } = await getFileDifferences(projectId, imageDiff, commits.parent_ids[0], commits.id);
        imagesToRender.push({
          previousVersionFileParsed: previousVersionFile,
          nextVersionFileParsed: nextVersionFile,
          fileName: imageDiff.old_path.split('/').slice(-1)[0],
        });
        this.setState({ ...imagesToRender });
      });
  }

  aprox = (floatValue) => Math.floor(floatValue);

  render() {
    const { projects } = this.props;
    const proj = projects.selectedProject;
    const groupName = proj.namespace.name;
    const {
      commits,
      users,
      imagesToRender,
      files,
    } = this.state;
    const commitId = commits.short_id;
    let avatarUrl = 'https://assets.gitlab-static.net/uploads/-/system/user/avatar/3839940/avatar.png';
    let avatarName = '';
    users.forEach((contributor) => {
      if (contributor.name === commits.author_name) {
        avatarUrl = contributor.avatar_url;
        avatarName = contributor.name;
      }
    });
    return (
      <div id="commits-view-container">
        <Navbar />
        <ProjectContainer activeFeature="data" folders={[groupName, proj.name, 'Data', 'Commits', commits.short_id]} />
        <br />
        <br />
        <div className="main-content">
          <div className="wrapper">
            <span className="commit-information">
              <span className="commit-authored">
                {'Commit '}
                <b>{commitId}</b>
                {` authored ${getTimeCreatedAgo(commits.authored_date, new Date())}`}
              </span>
              <a href={`/${avatarName}`}>
                <span style={{ position: 'relative' }}>
                  <img className="avatar-circle m-0" width="25" height="25" src={avatarUrl} alt="avatar" />
                </span>
              </a>
              <span className="author">
                <b>
                  <a href={`/${avatarName}`}>
                    {commits.author_name}
                  </a>
                </b>
              </span>
            </span>
            <div className="other-options">
              <Link to="#foo" className="btn btn-outline-dark px-3 mr-2">
                Browse Files
              </Link>
              <MDropdown
                label="Options"
              />
            </div>
          </div>
          <hr />
          <div className="commit-message">
            <span><b>{commits.title}</b></span>
            { getCommentFromCommit(commits.message) && (
              <div className="messages">
                <pre>{getCommentFromCommit(commits.message)}</pre>
              </div>
            )}
          </div>
          <hr />
          <p className="stats">
            {`Showing ${files.length} files changed with`}
            <span className="addition">
              {' '}
              {commits.stats ? commits.stats.additions : 0}
              {' '}
              additions
            </span>
            {' '}
            and
            <span className="deleted">
              {' '}
              {commits.stats ? commits.stats.deletions : 0}
              {' '}
              deletions
            </span>
            .
          </p>
          {imagesToRender.map((imageFile) => (
            <ImageDiffSection
              key={imageFile.fileName}
              fileInfo={imageFile}
              original={imageFile.previousVersionFileParsed}
              modified={imageFile.nextVersionFileParsed}
            />
          ))}
        </div>
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    projects: state.projects,
    users: state.users,
  };
}

const project = shape(
  {
    id: string,
    gid: number,
    description: string,
    name: string,
    avatarUrl: string,
    starCount: number,
    forksCount: number,
  },
);

CommitDetails.propTypes = {
  users: arrayOf(shape(
    {
      id: number,
      name: string,
      username: string,
      state: string,
      avatar_url: string,
      web_url: string,
    },
  )).isRequired,
  match: shape({
    path: string,
    url: string,
    isExact: bool,
    params: shape({
      projectId: string,
      commitId: string,
    }),
  }).isRequired,
  projects: shape({
    all: arrayOf(project),
    selectedProject: project,
  }).isRequired,
};
export default connect(mapStateToProps)(CommitDetails);
