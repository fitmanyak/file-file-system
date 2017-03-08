# file-file-system

This is library that implements file system on file.

## Entry points

There are two entry points to use library.

`FileFileSystem.format` formats given file to further using as file system with given size in bytes.
Size must be between **2 081** bytes and **2 216 203 124 237** bytes.  

> ### Warning
>
> All existing data in file may be lost.

`FileFileSystem.mount` mounts file formatted earlier with `FileFileSystem.format`.
It returns mounted file system as`IFileFileSystem`.

## `IFileFileSystem`

`IFileFileSystem` represents mounted file system.

### Methods

* `getTotalSpace` returns total space in bytes of this file system.
* `getFreeSpace` returns current free space in bytes of this file system.
* `getRootDirectory` returns root directory of this file system as `IRootDirectory`.
* `close` unmounts this file system.

> ### Note
>
> `IFileFileSystem` extends `Closeable`. So it may be auto-closed by try-with-resources statement.

## `IRootDirectory`

`IRootDirectory` simply extends `IDirectory` and adds nothing.

## `IDirectory`

`IDirectory` represents directory.

### Methods

* `isEmpty` returns whether this directory has child items.
* `isDirectory` always returns **`true`** for directories.
* `getName` returns this directory name. Always returns empty string for root directory.
* `setName` sets this directory name. Always fails for root directory.
* `getParentDirectory` returns this directory parent directory as `IDirectory`.
Always returns **`null`** for root directory.
* `createFile` creates file as child item with given name. It opens new file and returns as `IFile`.
* `openFile` opens existing child file with given name and returns as `IFile`.
* `createSubDirectory` creates directory as child item with given name.
It opens new directory and returns as `IDirectory`.
* `openSubDirectory` opens existing child directory with given name and returns as `IDirectory`.
* `openItem` opens existing child item with given name and returns as `IDirectoryItem`.
* `getNames` returns collection of child item names.
* `remove` removes this directory from its parent directory.
Always fails for root directory. Fails if this directory is not empty.
* `close` closes this directory opened earlier.

> ### Note
>
> `IDirectory` extends `Closeable`. So it may be auto-closed by try-with-resources statement.

## `IFile`

`IFile` represents file.

### Methods

* `isEmpty` returns whether this file is empty.
* `isDirectory` always returns **`false`** for files.
* `getName` returns this file name.
* `setName` sets this file name.
* `getParentDirectory` returns this file parent directory as `IDirectory`.
* `getPosition` returns this file current position.
* `setPosition` sets this file current position.
Fails if given position is greater than current file size.
* `reset` sets this file current position at the begin of file.
* `getSize` returns this file current size. 
* `setSize` sets this file size.
* `read` reads from this file to given buffer.
It may read starting at current file position or at given position.
Fails if given position is greater than current file size.
* `write` writes from given buffer to this file.
It may write starting at current file position or at given position.
Fails if given position is greater than current file size.
Increases this file size if needed.
* `remove` drops this file content and removes this file from its parent directory.
* `close` closes this file opened earlier.

> ### Note
>
> `IFile` extends `Closeable`. So it may be auto-closed by try-with-resources statement.

## `IDirectoryItem`

`IDirectoryItem` is base for `IDirectory` and `IFile`.

### Methods

* `isEmpty` returns whether this item is empty.
* `isDirectory` returns whether this item is directory.
* `getName` returns this item name. Always returns empty string for root directory.
* `setName` sets this item name. Always fails for root directory.
* `getParentDirectory` returns this item parent directory as `IDirectory`.
Always returns **`null`** for root directory.
* `remove` removes this item from its parent directory.
Always fails for root directory. Fails if this item is directory and it is not empty.
* `close` closes this item opened earlier.

> ### Note
>
> `IDirectoryItem` extends `Closeable`. So it may be auto-closed by try-with-resources statement.
