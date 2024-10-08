<?php

namespace Ioc;

use Psr\Container\ContainerExceptionInterface;

class DependencyException extends \Exception implements ContainerExceptionInterface
{
}
