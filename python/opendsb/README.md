# Opendsb

[![PyPI - Version](https://img.shields.io/pypi/v/opendsb.svg)](https://pypi.org/project/opendsb)
[![PyPI - Python Version](https://img.shields.io/pypi/pyversions/opendsb.svg)](https://pypi.org/project/opendsb)

-----

**Table of Contents**

- [Installation](#installation)
- [Build](#build)
- [Install](#install)
- [Package Publishing](#package-publishing)
- [License](#license)

## Installation

```console
python3 -m pip install -e .

# Verifying installation
python3 -m pip list | grep opendsb

# Build package
python3 -m build
# Now, you can install in another module using dist/opendsb-2.1.3.tar.gz
```

## Build using hatch

```console
hatch clean ; hatch build
hatch config show | grep 'data ='
hatch config show | grep 'data =' | awk '{ print $3  $4}'
tree -L 4 $HOME/.local/share/hatch/env/virtual/opendsb
``` 

## Package Publishing

```console
hatch publish
```

## License

`opendsb` is distributed under the terms of the [MIT](https://spdx.org/licenses/MIT.html) license.
