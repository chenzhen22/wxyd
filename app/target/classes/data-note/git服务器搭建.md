---
name: Git 服务器搭建
category: Git
---

# 搭建git服务器和客户端

## Git是什么？

Git是目前世界上最先进的分布式版本控制系统。

## SVN与Git的最主要的区别？

SVN是集中式版本控制系统，版本库是集中放在中央服务器的，而干活的时候，用的都是自己的电脑，所以首先要从中央服务器哪里得到最新的版本，然后干活，干完后，需要把自己做完的活推送到中央服务器。集中式版本控制系统是必须联网才能工作，如果在局域网还可以，带宽够大，速度够快，如果在互联网下，如果网速慢的话，就纳闷了。　　Git是分布式版本控制系统，那么它就没有中央服务器的，每个人的电脑就是一个完整的版本库，这样，工作的时候就不需要联网了，因为版本都是在自己的电脑上。既然每个人的电脑都有一个完整的版本库，那多个人如何协作呢？比如说自己在电脑上改了文件A，其他人也在电脑上改了文件A，这时，你们两之间只需把各自的修改推送给对方，就可以互相看到对方的修改了。

## 基本概念

工作区：就是你在电脑里能看到的目录。

暂存区：英文叫stage, 或**index**。一般存放在 ".git目录下" 下的**index**文件（.git/**index**）中，所以我们把暂存区有时也叫作索引（**index**）。工作区的文件先被增加到这个区域里，再从这个区域提交到版本库。

版本库：工作区有一个隐藏目录.git，这个不是工作区，而是Git的版本库。

## 安装git

```
我这里使用的yum安装[root@localhost ~]# yum install git也可以使用源码安装，需要安装很多依赖包
yum install -y dh-autoreconf curl-devel expat-devel gettext-devel openssl-devel perl-devel zlib-devel libxslt asciidoc xmlto docbook2X autoconf install-info getopt
```

```
ln -s /usr/bin/db2x_docbook2texi /usr/bin/docbook2x-texi
ln -s /usr/bin/db2x_docbook2texi /usr/bin/docbook2texi
```

下载地址 https://github.com/git/git/releases

```
$ cd /usr/src
$ mkdir git
$ cd git
$ wget -O git-2.35.1.tar.gz https://codeload.github.com/git/git/tar.gz/refs/tags/v2.35.1
$ tar -zxf git-2.35.1.tar.gz
$ cd git-2.35.1
$ make configure
$ ./configure --prefix=/usr
$ make all doc info
```

ln -s /usr/bin/db2x_docbook2texi /usr/bin/docbook2x-texi进行软连接

make all doc info再次执行。

ln -s /usr/bin/db2x_docbook2texi /usr/bin/docbook2texi进行再次软链接。

然后再执行make all doc info。

```
$ sudo make install install-doc install-html install-info
```

获取git版本，安装成功

```
[root@localhost ~]# **git** **version**  **#git**版本
**git** **version** 2.35.1
```

git安装好后，创建 git 用户，用来管理 Git 服务，并为 git 用户设置密码

```
[root@localhost ~]# useradd git
[root@localhost ~]# passwd git
```

禁止 git 用户 ssh 登录服务器，修改/etc/passwd文件git:x:500:500::/home/git:/usr/bin/git-shell

## 创建版本库

```
在文件夹下新建了一个文件夹名为tradeUnion[root@localhost git]# mkdir tradeUnion将tradeUnion初始化为git仓库[root@localhost tradeUnion]# git init该命令将创建一个名为 .git 的子目录，是隐藏的，这个子目录含有你初始化的 Git 仓库中所有的必须文件，这些文件是 Git 仓库的骨干。
[root@localhost tradeUnion]# ll -a
```

drwxr-xr-x.  8 root root 4096 1月   8 17:35 .git

```
我从网上查到，如果初始化远程仓库的话，要使用这个命令：git init --bare tradeUnion.git这样初始化的仓库并没有.git目录，只有.git目录下的文件。不使用--bare选项时,就会生成.git目录以及其下的版本历史记录文件,这些版本历史记录文件就存放在.git目录下;而使用--bare选项时,不再生成.git目录,而是只生成.git目录下面的版本历史记录文件,这些版本历史记录文件也不再存放在.git目录下面,而是直接存放在版本库的根目录下面用"git init"初始化的版本库用户也可以在该目录下执行所有git方面的操作。但别的用户在将更新push上来的时候容易出现冲突。解决办法就是使用”git init --bare”方法创建一个所谓的裸仓库，之所以叫裸仓库是因为这个仓库只保存git历史提交的版本信息，而不允许用户在上面进行各种git操作，如果你硬要操作的话，只会得到下面的错误（”This operation must be run in a work tree”），这个就是最好把远端仓库初始化成bare仓库的原因。说白了，就是会创建一个裸仓库，裸仓库没有工作区，服务器上的Git仓库纯粹是为了共享，所以不让用户直接登录到服务器上去改工作区，并且服务器上的Git仓库通常都以.git结尾。然后，把仓库所属用户改为git：[root@localhost Disk]# chown -R git:git tradeUnion.git
```

在windows上安装git作为客户端

## 安装git

下载地址：http://gitforwindows.org/直接安装即可，安装后在开始菜单里会有git bash、git GUI和git CMD

## 设置用户名、邮箱

因为Git是分布式版本控制系统，所以需要填写用户名和邮箱作为一个标识。每一个 Git 的提交都会使用这些信息，并且它会写入到你的每一次提交中，不可更改。如果使用了 --global 选项，那么该命令只需要运行一次，因为之后无论你在该系统上做任何事情， Git 都会使用那些信息。 当你想针对特定项目使用不同的用户名称与邮件地址时，可以在那个项目目录下运行没有 --global 选项的命令来配置。打开git bash如果想要检查你的配置，可以使用 git config --list 命令来列出所有 Git 当时能找到的配置。

```
root@localhost ~]# git config --list
user.name=xxx
user.email=xxx@qq.com
core.repositoryformatversion=0
core.filemode=true
core.bare=false
core.logallrefupdates=true
......
```

将linux服务端和windows客户端关联起来

## 客户端生成ssh私钥和公钥

想要他们连接的话要创建证书登录收集所有需要登录的用户的公钥，公钥位于id_rsa.pub文件中，把我们的公钥导入到/home/git/.ssh/authorized_keys文件里，一行一个。 打开windows的git bash，输入ssh-keygen -t rsa -C “邮箱”，生成ssh私钥和公钥

此时 C:\Users\用户名.ssh 下会多出两个文件 id_rsa 和 id_rsa.pub

id_rsa 是私钥id_rsa.pub 是公钥

## 服务器端 Git 打开 RSA 认证

进入 /etc/ssh 目录，编辑 sshd_config，打开以下三个配置的注释：

```
RSAAuthentication yes
PubkeyAuthentication yes
AuthorizedKeysFile .ssh/authorized_keys
保存并重启 sshd 服务：[root@localhost ssh]# /etc/rc.d/init.d/sshd restart
```

## 将客户端公钥加到服务器

由 AuthorizedKeysFile 得知公钥的存放路径是 .ssh/authorized_keys，实际上是 $Home/.ssh/authorized_keys，由于管理 Git 服务的用户是 git，所以实际存放公钥的路径是 /home/git/.ssh/authorized_keys在 /home/git/ 下创建目录 .ssh

```
[root@localhost git]# pwd
/home/git
[root@localhost git]# mkdir .ssh
[root@localhost git]# ls -a
. .. .bash_logout .bash_profile .bashrc .gnome2 .mozilla .ssh
```

然后把 .ssh 文件夹的 owner 修改为 git，为.ssh和authorized_keys修改权限

```
[root@localhost git]# chown -R git:git .ssh
[root@localhost git]# chmod 700 .ssh
[root@localhost git]# touch .ssh/authorized_keys
[root@localhost git]#chmod 600 .ssh/authorized_keys
```

将客户端公钥id_rsa.pub文件的内容写到服务器端 /home/git/.ssh/authorized_keys 文件里

## 在客户端clone远程仓库

将服务器上的/opt/repository/gittest.git库克隆到本地 git clone [URL]git是用户名@服务器地址：仓库路径可以看到本地上的服务器仓库了

## 在客户端远程仓库并提交代码

你已经在本地创建了一个Git仓库后，又想在服务器创建一个Git仓库，并且让这两个仓库进行远程同步先在linux本地init一个仓库，在里面随意编辑一个文件，在本地提交，然后连接远程仓库，将文件push到服务器上，由于远程仓库是没有工作目录的，所以看不到你提交的文件，但是在服务器上git log一下，可以看到你提交的历史记录

远程库的名字就是origin，这是Git默认的叫法，也可以改成别的。把本地库的内容推送到远程，用git push命令，实际上是把当前分支master推送到远程。由于远程库是空的，我们第一次推送master分支时，加上了-u参数，Git不但会把本地的master分支内容推送的远程新的master分支，还会把本地的master分支和远程的master分支关联起来，在以后的推送或者拉取时就可以简化命令，git push origin master就行要查看远程库的信息 使用 git remote它会列出你指定的每一个远程服务器的简写。 如果你已经克隆了自己的仓库，那么至少应该能看到 origin - 这是 Git 给你克隆的仓库服务器的默认名字要查看远程库的详细信息 使用 git remote –v会显示需要读写远程仓库使用的 Git 保存的简写与其对应的 URL，如果你的远程仓库不止一个，该命令会将它们全部列出

## tortoiseGit的安装使用

## 安装

下载安装包，选择版本，地址：https://download.tortoisegit.org/tgit/进入具体版本页面后,根据Windows操作系统版本选择相应的程序安装包和中文语言包.https://download.tortoisegit.org/tgit/2.5.0.0/ 这是2.5.0.0版本的，直接安装即可

## 设置

在桌面右键选择tortoiseGit，选择设置，设置用户名和邮箱，跟在git bash是一样的，此时设置的是全局的进到本地仓库目录中可以针对项目设置局部配置

## 克隆仓库

ssh key的步骤前面已经说完了，然后就可以克隆仓库进行使用了选择你想要克隆仓库的文件夹，右键选择Git克隆，等同于 git clone [url]URL是你要克隆的服务器版本库路径，目录是你要克隆到你本地的路径填写服务器的url，确定，在克隆过程中会要求输入git用户的密码，然后在本地生成了仓库gittest，在gittest下面有.git目录，就是本地的版本库目录了，如果上一步选择了克隆成纯版本库，就不会有.git目录了，在本地一般不这样做

## 增加文件到暂存区

编辑一个文件保存，在空白处右键选择TortoiseGit，选择添加，加到暂存区，也就是git add，也可以在下一步里面直接提交

## 本地提交文件

右键选择Git提交，填写日志信息，不然不能提交然后点提交，这一步是将文件提交到你本地的仓库，git中不能提交一个空文件夹，里面一定要有文件才行第一行默认提交到master分支，也可以选择新建分支，提交到新分支上没有添加的文件也可以直接提交到版本库，也可以选择不提交未添加的文件

## 推送到远程仓库

将文件推送到远程仓库，右键选择tortoiseGit，选择推送，将文件推送到服务器仓库上可以选择推送哪个分支上的信息，也可以选择推送到哪个远程分支上目标：选择推送到哪个远程端上，选择管理，可以增加新的远程端，也可以直接写别的URL确定推送在服务器仓库下执行git log可以看到你推送的文件，因为服务器上是裸仓库，所以没有工作目录，看不到推送的文件

## 拉取文件和获取文件

拉取和获取的区别拉取：git pull 在将远程仓库最新版本拉到本地的同时，将其合并到本地的当前 HEAD 中。一般选择非fast forward，会多生成一个commit 记录，并强制保留分支的开发记录，不会丢失分支信息，这对于以后代码进行分析特别有用。获取：git fetch 将远程仓库最新版本拉到本地，不会自动合并 ，绝不会更改任何本地分支实际使用中 使用git fetch 更安全 在merge之前可以看清楚 更新情况 再决定是否合并

创建分支，切换分支

右键TortoiseGit，选择创建分支，填入分支名称，选择在哪个分支上新建分支，如果勾选切换新分支，分支创建后就直接在新分支上工作了，否则在TortoiseGit中选择切换分支在分支上修改后提交到分支

## 合并

切换回master分支，TortoiseGit选择合并，选择非快进式合并，合并后master分支上有了其他分支上提交的信息

## 解决冲突

当主干与分支修改了同一个文件，合并时会产生冲突

打开文件与在命令行中一样，用<<<<<<<，=======，>>>>>>>标记出不同分支的内容右键TortoiseGit中选择解决冲突双击该文件进行修改，左边窗口是分支上的内容，右边窗口是master上的内容，你要在下面的窗口上解决冲突，修改正确的文件内容，然后保存，然后将解决后的文件进行提交

## 显示日志

红色代表当前分支，绿色代表本地分支，浅×××代表远端分支，×××代表标签可以看到各版本提交的信息，及分支合并的信息，还有文件的操作状态颜色可以在设置中更改可以在每一条信息上右键选择与上一版本比较差异

以上就是TortoiseGit的基本操作了下面是一些命令行操作

## svn迁移到git服务器

根据需要，后期将会将svn上的数据迁移到git上

## 将svn库克隆到本地

使用TortoiseGit克隆svn库URL是svn的地址，是你要克隆的文件夹因为我的svn库不是标准的trunk、branch、tags结构，所以下面的不勾选，如果是标准的svn结构，请勾选克隆需要一段时间，根据svn库的大小而定

## 在git服务器上创建新的裸仓库

使用git init --bare svnrepo.git命令

## 将本地仓库推送到git服务器

先建立远端仓库，在本地仓库内右键选择TortoiseGit，设置，然后选择远端，在右侧URL中填写git服务器仓库地址，就是刚刚在git服务器上新建的裸仓库，origin是默认的远端名字，然后点击确定远端建立好后，就可以将本地克隆到的仓库推送到服务器上了在本地仓库做一些修改，记住要先在本地进行提交，然后再推送到服务器上TortoiseGit—>推送 就可以了，由于服务器上是裸仓库，所以没有工作区，看不到文件，git log一下就可以看到你刚才修改文件的提交记录了

## 基础命令

我是在linux上操作的，算是本地仓库吧，不涉及远程库；在linux上操作与在windows上相同

## 把文件添加到版本库中

```
编辑一个readme.txt文件，将文件加入版本库中[root@localhost repository]# git add readme.txt #将工作区中的文件添加到暂存区了
```

## 将文件提交到版本库

```
[root@localhost repository]# git commit -m “提交readme”-m 是提交信息，便于查看提交了什么东西，这一步是将暂存区中的文件提交到仓库了
```

## 查看是否还有文件未提交

```
[root@localhost repository]# git status
#On branch master
nothing to commit (working directory clean)
```

这样说明没有任何文件未提交，工作区是干净的

修改readme文件后保存，不进行任何操作，再查看状态

```
[root@localhost repository]# git status
#On branch master
#Changed but not updated:
#(use "git add <file>..." to update what will be committed)
#   (use "git checkout -- <file>..." to discard changes in working directory)
#
#       modified:   readme.txt
#
**no** changes added to commit (**use** "git add" **and**/**or** "git commit -a")
```

上面的命令告诉我们 readme.txt文件已被修改，但是并没有提交

查看readme.txt文件修改了什么内容

```
[root@localhost repository]# git diff
diff --git a/readme.txt b/readme.txt
index 92e045e..d94ee74 100644
--- a/readme.txt
+++ b/readme.txt
**@@ -5,4 +5,3 @@**
666666
777777
999999
-000
```

可以看到，少了一行000知道了对readme.txt文件做了什么修改后，我们可以放心的提交到仓库了，提交修改和提交文件是一样的2步(第一步是git add 第二步是：git commit)。

```
[root@localhost repository]# git add readme.txt
[root@localhost repository]# git commit -m "readme删除000"
```

## 查看历史记录

```
[root@localhost repository]# git log
```

commit ffc46d25feacd0ae1926ace37efecd116402503e    #版本号

```
Author: bai <xxx@qq.com>
Date:   Tue Jan **9** **16**:**49**:**18** **2018** +080**0**
```

readme删除000       #最近一次提交内容，也就是commit -m 写的内容

```
commit 1fac1a93037444bfbd59e51b57c511aaa0a9578f
Author: bai <xxx@qq.com>
Date:   Mon Jan **8** **11**:**26**:**26** **2018** +080**0**
```

## readme增加222222

```
commit 876477f1b209ba61de4248ff26c2c1a098520295
Author: bai <xxx@qq.com>
Date:   Mon Jan **8** **11**:**20**:**45** **2018** +080**0**
```

## 提交readme.txt

如果嫌上面显示的信息太多的话，我们可以使用命令 git log –pretty=oneline

```
[root@localhost repository]# git log --pretty=oneline
```

ffc46d25feacd0ae1926ace37efecd116402503e readme删除000

1fac1a93037444bfbd59e51b57c511aaa0a9578f readme增加222222

876477f1b209ba61de4248ff26c2c1a098520295 提交readme.txt

## 版本回退

```
[root@localhost repository]# git reset --hard HEAD^ #回退到上一版本
[root@localhost repository]# git log
commit 1fac1a93037444bfbd59e51b57c511aaa0a9578f
Author: bai <xxx@qq.com>
Date:   Mon Jan **8** **11**:**26**:**26** **2018** +080**0**
```

## readme增加222222

```
commit 876477f1b209ba61de4248ff26c2c1a098520295
Author: bai <xxx@qq.com>
Date:   Mon Jan **8** **11**:**20**:**45** **2018** +080**0**
```

## 提交readme.txt

可以看到删除000的操作没有了如果想要回退到上上个版本就把HEAD^ 改成 HEAD^^，以此类推如果想回到100个版本之前的话，就git reset --hard HEAD~100

## 回退到指定版本

```
[root@localhost repository]# git reset HEAD 版本号
```

## 获得版本号

```
[root@localhost repository]# git reflog
19ecf86 HEAD@{0}: HEAD^: updating HEAD
```

ffc46d2 HEAD@{1}: commit: readme删除000

## ffc46d2就是版本号

## 撤销修改

```
1.如果已经提交了的话，就直接回退到上一版本2.如果没有添加到暂存区的话，使用git checkout -- 文件名 撤销工作区的所有修改[root@localhost repository]# git checkout -- readme.txt3.如果已经添加到暂存区的话，想丢弃修改[root@localhost repository]# git reset HEAD readme.txt然后再重复第二步
```

## 删除文件

```
[root@localhost repository]# git rm readme.txt
[root@localhost repository]# git commit -m “删除readme”
```

## 创建分支

```
[root@localhost repository]# git checkout –b fenzhi
```

## 这个命令相当于两条命令

git branch fenzhi   创建分支

git checkout fenzhi   切换到该分支

我们在分支上修改了文件内容提交后，在主分支上是看不到刚才修改的内容的

## 查看当前分支

```
[root@localhost repository]# git branch
* fenzhi
master
```

## 带星号的则代表当前在该分支上

## 合并分支

```
[root@localhost repository]# git checkout master   #在master分支上进行合并
[root@localhost repository]# git merge --no-ff fenzhi
```

在分支上修改内容后，与主分支合并，合并后就统一了文件内容

## 删除分支

```
[root@localhost repository]# git branch -d fenzhi
```

## 解决冲突

当你在主分支上和其他分支上修改了同一个文件并且都提交了，当在主分支上合并的时候就会产生冲突查看文件内容是这样的Git用<<<<<<<，=======，>>>>>>>标记出不同分支的内容，其中<<< HEAD是指主分支修改的内容，>>>>fenzhi 是指fenzhi上修改的内容我们可以手动修改文件，解决冲突，然后在提交到版本库

## 用git log查看分支历史

```
git log --graph --pretty=oneline --abbrev-commit
```

## 隐藏工作区

在开发中，会经常碰到bug问题，那么有了bug就需要修复，每个bug都可以通过一个临时分支来修复，修复完成后，合并分支，然后将临时的分支删除掉。比如我在开发中接到一个404 bug时候，我们可以创建一个404分支来修复它，但是，当前的dev分支上的工作还没有提交。工作进行到一半时候，我们还无法提交，比如我这个分支bug要2天完成，但是404 bug需要5个小时内完成。怎么办呢？还好，Git还提供了一个stash功能，可以把当前工作现场 ”隐藏起来”，等以后恢复现场后继续工作。在你需要隐藏的dev分支下执行git stash，这时候虽然dev分支上的内容还没有提交，但是git status看一下工作区是干净的，也就是dev的工作现场被隐藏了现在创建404分支来修复bug了。首先我们要确定在哪个分支上修复bug，比如我现在是在主分支master上来修复的，现在我要在master分支上创建一个临时分支修复完成后，切换到master分支上，并完成合并，最后删除404分支。然后我们回到dev分支上干活，工作区是干净的，那么我们工作现场去哪里呢？我们可以使用命令 git stash list来查看下工作区是干净的，那么我们工作现场去哪里呢？我们可以使用命令 git stash list来查看下。Git把stash内容存在某个地方了，但是需要恢复一下，可以使用如下2个方法：git stash apply恢复，恢复后，stash内容并不删除，你需要使用命令git stash drop来删除。另一种方式是使用git stash pop,恢复的同时把stash内容也删除了。
