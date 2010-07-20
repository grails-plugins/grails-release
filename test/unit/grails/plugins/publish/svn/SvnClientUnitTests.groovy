package grails.plugins.publish.svn

import grails.test.GrailsUnitTestCase

import org.gmock.WithGMock
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNErrorCode
import org.tmatesoft.svn.core.SVNErrorMessage
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.SVNNodeKind
import org.tmatesoft.svn.core.SVNProperties
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.wc.SVNCommitClient
import org.tmatesoft.svn.core.wc.SVNCopyClient
import org.tmatesoft.svn.core.wc.SVNCopySource
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNStatusClient
import org.tmatesoft.svn.core.wc.SVNUpdateClient
import org.tmatesoft.svn.core.wc.SVNWCClient
import org.tmatesoft.svn.core.wc.SVNWCUtil

@WithGMock
class SvnClientUnitTests extends GrailsUnitTestCase {
    def repoUrl = "http://svn.codehaus.org/grails-plugins"
    def mockAuthMgr = new Expando()

    void testConstructorWithNoCredentials() {
        mockClientConstruction()

        play {
            def testClient = new SvnClient(repoUrl)
            assertNotNull "Repository URL is null.", testClient.repoUrl
            assertNotNull "Authentication manager is null.", testClient.authManager
        }
    }

    void testConstructorWithCredentials() {
        def mockUtil = mock(SVNWCUtil)
        mockUtil.static.createDefaultAuthenticationManager("dilbert", "password").returns(mockAuthMgr)

        play {
            def testClient = new SvnClient("http://dilbert:password@svn.codehaus.org/grails-plugins")
            assertNotNull "Repository URL is null.", testClient.repoUrl
            assertNotNull "Authentication manager is null.", testClient.authManager
        }
    }


    void testSetCredentials() {
        def mockUtil = mockClientConstruction()
        mockUtil.static.createDefaultAuthenticationManager("dilbert", "password")

        play {
            def testClient = new SvnClient(repoUrl)
            testClient.setCredentials("dilbert", "password")
        }
    }

    void testIsWorkingCopyForRepository() {
        def relPath = "grails-stuff/trunk"

        mockClientConstruction()

        def wcDir = new File("tmp")
        def mockWcClient = mock(SVNWCClient, constructor(mockAuthMgr, null))
        mockWcClient.doInfo(wcDir.canonicalFile, SVNRevision.HEAD).returns([ URL: createSvnUrl(repoUrl + "/" + relPath) ])

        play {
            def testClient = new SvnClient(repoUrl)
            assertTrue testClient.isWorkingCopyForRepository(wcDir, relPath)
        }
    }

    void testIsWorkingCopyForRepositoryNoMatch() {
        mockClientConstruction()

        def wcDir = new File("tmp")
        def mockWcClient = mock(SVNWCClient, constructor(mockAuthMgr, null))
        mockWcClient.doInfo(wcDir.canonicalFile, SVNRevision.HEAD).returns([ URL: createSvnUrl(repoUrl + "/grails-stuff/trunk") ])

        play {
            def testClient = new SvnClient(repoUrl)
            assertFalse testClient.isWorkingCopyForRepository(wcDir, "grails-stuff/tags/LATEST_RELEASE")
        }
    }

    void testCheckOut() {
        def relPath = "grails-stuff/trunk"
        def wcDir = new File("tmp")

        mockClientConstruction()

        def mockUpdateClient = mock(SVNUpdateClient, constructor(mockAuthMgr, null))
        mockUpdateClient.doCheckout(createSvnUrl("$repoUrl/$relPath"), wcDir, SVNRevision.HEAD, SVNRevision.HEAD, true)

        play {
            def testClient = new SvnClient(repoUrl)
            testClient.checkOut(wcDir, relPath)
        }
    }

    void testUpdate() {
        def wcDir = new File("tmp")

        mockClientConstruction()

        def mockUpdateClient = mock(SVNUpdateClient, constructor(mockAuthMgr, null))
        mockUpdateClient.doUpdate(wcDir.canonicalFile, SVNRevision.HEAD, SVNDepth.INFINITY, false, false)

        play {
            def testClient = new SvnClient(repoUrl)
            testClient.update(wcDir)
        }
    }

    void testCommit() {
        def wcDir = new File("tmp")
        def firstArg = [ wcDir.canonicalFile ] as File[]

        mockClientConstruction()

        def mockCommitClient = mock(SVNCommitClient, constructor(mockAuthMgr, null))
        mockCommitClient.doCommit(firstArg, false, "Test commit", true, true)

        play {
            def testClient = new SvnClient(repoUrl)
            testClient.commit(wcDir, "Test commit")
        }
    }

    void testAddFilesToSvn() {
        def wcDir = new File("tmp")
        def files = [
                new File(wcDir, "file1"),
                new File(wcDir, "file2"),
                new File(wcDir, "file3"),
                new File(wcDir, "file4"),
                new File(wcDir, "file5") ]

        mockClientConstruction()

        def mockWcClient = mock(SVNWCClient, constructor(mockAuthMgr, null))
        mockWcClient.doAdd(files[1], true, false, false, false)
        mockWcClient.doAdd(files[3], true, false, false, false)
        mockWcClient.doAdd(files[4], true, false, false, false)

        def mockStatusClient = mock(SVNStatusClient, constructor(mockAuthMgr, null))
        mockStatusClient.doStatus(files[0], true).returns([ kind: SVNNodeKind.FILE ])
        mockStatusClient.doStatus(files[1], true).returns([ kind: SVNNodeKind.UNKNOWN ])
        mockStatusClient.doStatus(files[2], true).returns([ kind: SVNNodeKind.DIR ])
        mockStatusClient.doStatus(files[3], true).returns([ kind: SVNNodeKind.NONE ])
        mockStatusClient.doStatus(files[4], true).raises(new SVNException(SVNErrorMessage.create(SVNErrorCode.BASE)))

        play {
            def testClient = new SvnClient(repoUrl)
            testClient.addFilesToSvn(files)
        }
    }

    void testTag() {
        def wcDir = new File("tmp")

        mockClientConstruction()

        def testCommitMsg = "Creating tag 'my-tag'."
        def mockCommitClient = mock(SVNCommitClient, constructor(mockAuthMgr, null))
        // The client should attempt to create the base URL for tags.
        mockCommitClient.doMkDir(createSvnUrl(repoUrl + "/grails-stuff/tags") , testCommitMsg)

        // It should then attempt to delete the required tag in case
        // it already exists.
        mockCommitClient.doDelete(createSvnUrl(repoUrl + "/grails-stuff/tags/my-tag"), testCommitMsg)

        def mockCopyClient = mock(SVNCopyClient, constructor(mockAuthMgr, null))
        mockCopyClient.doCopy(
                match { it.size() == 1 && it[0].getURL() == createSvnUrl(repoUrl + "/grails-stuff/trunk") },
                createSvnUrl(repoUrl + "/grails-stuff/tags/my-tag"),
                false,
                false,
                true,
                testCommitMsg,
                match { it })

        play {
            def testClient = new SvnClient(repoUrl)
            testClient.tag("grails-stuff/trunk", "grails-stuff/tags", "my-tag", testCommitMsg)
        }
    }

    void testTagWithExceptions() {
        def wcDir = new File("tmp")

        mockClientConstruction()

        def testCommitMsg = "Creating tag 'my-tag'."
        def mockCommitClient = mock(SVNCommitClient, constructor(mockAuthMgr, null))
        // The client should attempt to create the base URL for tags.
        mockCommitClient.doMkDir(createSvnUrl(repoUrl + "/grails-stuff/tags") , testCommitMsg).raises(
                new SVNException(SVNErrorMessage.create(SVNErrorCode.BASE)))

        // It should then attempt to delete the required tag in case
        // it already exists.
        mockCommitClient.doDelete(createSvnUrl(repoUrl + "/grails-stuff/tags/my-tag"), testCommitMsg).raises(
                new SVNException(SVNErrorMessage.create(SVNErrorCode.BASE)))

        def mockCopyClient = mock(SVNCopyClient, constructor(mockAuthMgr, null))
        mockCopyClient.doCopy(
                match { it.size() == 1 && it[0].getURL() == createSvnUrl(repoUrl + "/grails-stuff/trunk") },
                createSvnUrl(repoUrl + "/grails-stuff/tags/my-tag"),
                false,
                false,
                true,
                testCommitMsg,
                match { it })

        play {
            def testClient = new SvnClient(repoUrl)
            testClient.tag("grails-stuff/trunk", "grails-stuff/tags", "my-tag", testCommitMsg)
        }
    }

    void testPathExists() {
        mockClientConstruction()

        def testPath = "/grails-stuff/trunk"
        def mockRepo = mock()
        mockRepo.authenticationManager.set(match { it })
        mockRepo.info(testPath, -1).returns([:])

        def mockRepoFactory = mock(SVNRepositoryFactory)
        mockRepoFactory.static.create(createSvnUrl(repoUrl)).returns(mockRepo)

        play {
            def testClient = new SvnClient(repoUrl)
            assertTrue "pathExists() did not return true!", testClient.pathExists(testPath)
        }
    }

    void testPathExistsWithNonExistentPath() {
        mockClientConstruction()

        def testPath = "/grails-stuff/trunk"
        def mockRepo = mock()
        mockRepo.authenticationManager.set(match { it })
        mockRepo.info(testPath, -1).returns(null)

        def mockRepoFactory = mock(SVNRepositoryFactory)
        mockRepoFactory.static.create(createSvnUrl(repoUrl)).returns(mockRepo)

        play {
            def testClient = new SvnClient(repoUrl)
            assertFalse "pathExists() did not return false!", testClient.pathExists(testPath)
        }
    }

    void testCreatePath() {
        mockClientConstruction()

        def testPath = "some/new/path"
        def testCommitMsg = "Creating new path."
        def mockCommitClient = mock(SVNCommitClient, constructor(mockAuthMgr, null))
        // The client should attempt to create the path using doMkDir().
        def firstArg = [ createSvnUrl(repoUrl + "/$testPath") ] as SVNURL[]
        mockCommitClient.doMkDir(firstArg, testCommitMsg, new SVNProperties(), true)

        play {
            def testClient = new SvnClient(repoUrl)
            testClient.createPath(testPath, testCommitMsg)
        }
    }

    void testFetchFile() {
        mockClientConstruction()

        def testPath = "some/new/path"
        def mockOutputStream = new ByteArrayOutputStream()

        def mockRepo = mock()
        mockRepo.authenticationManager.set(match { it })
        mockRepo.checkPath(testPath, -1).returns(SVNNodeKind.FILE)
        mockRepo.getFile(testPath, -1L, new SVNProperties(), mockOutputStream)

        def mockRepoFactory = mock(SVNRepositoryFactory)
        mockRepoFactory.static.create(createSvnUrl(repoUrl)).returns(mockRepo)

        def localFile = new Expando()
        localFile.withOutputStream = { c -> c.call(mockOutputStream) }

        play {
            def testClient = new SvnClient(repoUrl)
            testClient.fetchFile(testPath, localFile)
        }
    }

    void testFetchFileRemotePathNonExistent() {
        mockClientConstruction()

        def testPath = "some/new/path"
        def mockOutputStream = new ByteArrayOutputStream()

        def mockRepo = mock()
        mockRepo.authenticationManager.set(match { it })
        mockRepo.checkPath(testPath, -1).returns(SVNNodeKind.NONE)

        def mockRepoFactory = mock(SVNRepositoryFactory)
        mockRepoFactory.static.create(createSvnUrl(repoUrl)).returns(mockRepo)

        play {
            def testClient = new SvnClient(repoUrl)
            def msg = shouldFail {
                testClient.fetchFile(testPath, new Expando())
            }

            assertEquals "The remote file does not exist: $repoUrl/$testPath", msg
        }
    }

    void testFetchFileRemotePathIsNotFile() {
        mockClientConstruction()

        def testPath = "some/new/path"
        def mockOutputStream = new ByteArrayOutputStream()

        def mockRepo = mock()
        mockRepo.authenticationManager.set(match { it })
        mockRepo.checkPath(testPath, -1).returns(SVNNodeKind.DIR)

        def mockRepoFactory = mock(SVNRepositoryFactory)
        mockRepoFactory.static.create(createSvnUrl(repoUrl)).returns(mockRepo)

        play {
            def testClient = new SvnClient(repoUrl)
            def msg = shouldFail {
                testClient.fetchFile(testPath, new Expando())
            }

            assertEquals "The remote path is not a file: $repoUrl/$testPath", msg
        }
    }

    void testGetLatestRevision() {
        mockClientConstruction()

        def latestRevision = 1001
        def mockRepo = mock()
        mockRepo.authenticationManager.set(match { it })
        mockRepo.latestRevision.returns(latestRevision)

        def mockRepoFactory = mock(SVNRepositoryFactory)
        mockRepoFactory.static.create(createSvnUrl(repoUrl)).returns(mockRepo)

        play {
            def testClient = new SvnClient(repoUrl)
            assertEquals latestRevision, testClient.latestRevision
        }
    }

    private mockClientConstruction() {
        def mockUtil = mock(SVNWCUtil)
        mockUtil.static.createDefaultAuthenticationManager().returns(mockAuthMgr)

        return mockUtil
    }

    private createSvnUrl(url) {
        return SVNURL.parseURIDecoded(url)
    }
}
