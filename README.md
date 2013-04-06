# testlink2tfs

testlink2tfs is a tool designed to migrate test cases from TestLink to TFS. The following fields are migrated:
- Test Case name
- Summary (TestLink) -> Description (TFS) (migrates images too!)
- Steps + Expected results (TestLink) -> Steps (Action column) in TFS

Note: Attachments are not migrated, test suite hierarchy in TestLink is not preserved!

## Installation

Download from [SourceForge](https://sourceforge.net/projects/testlink2tfs/files/latest/download).

## Usage

First you need to create the config file to be used:

```yaml
tl: 
    url:         http://testlink-server/testlink/lib/api/xmlrpc.php
    tsid:        00000                             # root test suite id
    devkey:      00000000000000000000000000000000  # deb key for test link
    www-prefix:  http://testlink-server            # base directory for test link
    norec:       true                              # no recursive (will migrate only direct child items for tsid,
                                                   #    otherwise will plain inport all children without preserving test suite hierarchy)
tfs: 
    url:         https://tfs.yourcompany.com/tfs/DefaultCollection      # tfs collection url
    project:     Test_Project                                           # tfs project
    login:       domain\user                                            # credentials
    password:    yourpassword                                           #
```

The plain running of jar looks like:

    $ java -Dcom.microsoft.tfs.jni.native.base-directory=redist\native -jar testlink2tfs-0.1.0-standalone.jar tl2tfs.conf

For convenience you can use the testlink2tfs.bat file:

    $ testlink2tfs.bat tl2tfs.conf

## Options

    $ testlink2tfs config-path [case-limit]

- config-path: path to config file (required)
- case-limit: number of testcases to import (optional; mostly useful for testing)

### Bugs

- no high or critical severity issues

If you find any issues or have suggestions for improvements please use [issue tracker](https://github.com/ashenwolf/testlink2tfs/issues).

## License

Copyright © 2013 Materialise Dental Ukraine

Distributed under the Eclipse Public License, the same as Clojure.
