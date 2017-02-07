# Contributing to TranSMART

Are you ready to contribute to TranSMART? We'd love to have you on board, and we will help you as much as we can. Here are the guidelines we'd like you to follow so that we can be of more help:

 - [Getting Started](#start)
 - [Contributing Code Changes via a Pull Request](#pull-request)
 - [Automated tests on Travis CI](#test-travis)
 - [Coding Rules](#rules)
 - [Git Commit Guidelines](#commit)
 - [Additional resources](#additional-resources)

## <a name="start"></a> Getting Started
* Make sure you have a [GitHub account](https://github.com/signup/free).
* ???

## <a name="pull-request"></a> Contributing Code Changes via a Pull Request

### Submitting a Pull Request
Before you submit your pull request consider the following guidelines:

* Search the [TranSMART](https://github.com/???/pulls) repository for an open or closed Pull Request
  that relates to your submission.
* Make your changes in a new git branch

     ```shell
     git checkout -b [name_of_your_new_branch] master
     ```

* Create your patch, **including appropriate test cases**.
* Follow our [Coding Rules](#rules).

* Commit your changes using a descriptive commit message that follows our [commit message conventions](#commit-message-format).

     ```shell
     git commit -a
     ```

  Note: the optional commit `-a` command line option will automatically "add" and "rm" edited files.

* Push your branch to GitHub:

    ```shell
    git push origin [name_of_your_new_branch]
    ```

* In GitHub, send a pull request to `???:master`.
* If we suggest changes then
  * Make the required updates.
  * Re-run the tests on the project and ensure tests are still passing.
  * Rebase your branch and force push to your GitHub repository (this will update your Pull Request):

    ```shell
    git rebase master -i
    git push -f
    ```

That's it! Thank you for your contribution!

#### Resolving merge conflicts ("This branch has conflicts that must be resolved")

Sometimes your PR will have merge conflicts with the upstream repository's master branch. There are several ways to solve this but if not done correctly this can end up as a true nightmare. So here is one method that works quite well.

* First, fetch the latest information from the master

    ```shell
    git fetch upstream
    ```

* Rebase your branch against the upstream/master

    ```shell
    git rebase upstream/master
    ```

* Git will stop rebasing at the first merge conflict and indicate which file is in conflict. Edit the file, resolve the conflict then

    ```shell
    git add <the file that was in conflict>
    git rebase --continue
    ```

* The rebase will continue up to the next conflict. Repeat the previous step until all files are merged and the rebase ends successfully.
* Re-run the tests on your project to ensure tests are still passing.
* Force push to your GitHub repository (this will update your Pull Request)

    ```shell
    git push -f
    ```

#### After your pull request is merged

After your pull request is merged, you can safely delete your branch and pull the changes
from the main (upstream) repository:

* Delete the remote branch on GitHub either through the GitHub web UI or your local shell as follows:

    ```shell
    git push origin --delete [name_of_your_new_branch]
    ```

* Check out the master branch:

    ```shell
    git checkout master -f
    ```

* Delete the local branch:

    ```shell
    git branch -D [name_of_your_new_branch]
    ```

* Update your master with the latest upstream version:

    ```shell
    git pull --ff upstream master
    ```

## <a name="test-travis"></a> Automated tests on Travis CI
All Pull Requests are automatically tested on [Travis CI](https://travis-ci.org/???). Currently there is a set of tests for the core modules:
* Tests for transmart-core-db module against H2 database, 
* Tests for transmart-rest-api module, 
* Tests for transmart-core-db module against postgres database,
* Tests for transmart-batch module. 
## <a name="rules"></a> Coding Rules
To ensure consistency throughout the source code, keep these rules in mind as you are working:

* All features or bug fixes **must be tested** by one or more tests.
* Java files **must be** formatted using [Intellij IDEA's code style](http://confluence.jetbrains.com/display/IntelliJIDEA/Code+Style+and+Formatting).
* Web apps JavaScript files **must follow** [Google's JavaScript Style Guide](https://google-styleguide.googlecode.com/svn/trunk/javascriptguide.xml).

## <a name="commit"></a> Git Commit Guidelines

We have rules over how our git commit messages must be formatted. Please ensure to [squash](https://help.github.com/articles/about-git-rebase/#commands-available-while-rebasing) unnecessary commits so that your commit history is clean.

### <a name="commit-message-format"></a> Commit Message Format
Each commit message consists of a **header**, a **body** and a **footer**.

```
<header>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

Any line of the commit message cannot be longer 100 characters! This allows the message to be easier
to read on Github as well as in various git tools.

### Header
The Header contains a succinct description of the change:

* use the imperative, present tense: "change" not "changed" nor "changes"
* don't capitalize first letter
* no dot (.) at the end

### Body
If your change is simple, the Body is optional.

Just as in the Header, use the imperative, present tense: "change" not "changed" nor "changes".
The Body should include the motivation for the change and contrast this with previous behavior.

### Footer
The footer is the place to reference GitHub issues that this commit **Closes**.

You **must** use the [Github keywords](https://help.github.com/articles/closing-issues-via-commit-messages) for
automatically closing the issues referenced in your commit.

### Example
For example, here is a good commit message:

```
upgrade to Spring Boot 1.1.7

upgrade the Maven and Gradle builds to use the new Spring Boot 1.1.7,
see http://spring.io/blog/2014/09/26/spring-boot-1-1-7-released

Fix #1234
```
## <a name="additional-resources"></a>Additional Resources
* [??? documentation](???)
* [General GitHub documentation](http://help.github.com/)
* [GitHub pull request documentation](http://help.github.com/send-pull-requests/)