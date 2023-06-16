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
pip install opendsb
```

## Build

```console
hatch clean ; hatch build
hatch config show | grep 'data ='
hatch config show | grep 'data =' | awk '{ print $3  $4}'
``` 

## Install

```console
python3 -m pip install -e .
``` 

## Package Publishing

```console
hatch publish
```

## License

`opendsb` is distributed under the terms of the [MIT](https://spdx.org/licenses/MIT.html) license.
