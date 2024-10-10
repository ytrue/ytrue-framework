<?php


namespace Ioc;

use Ioc\Cache\CompiledContainerCache;
use Ioc\Compiler\Compiler;
use Ioc\Definition\Helper\CreateDefinitionHelper;
use Ioc\Definition\Source\AttributeBasedAutowiring;
use Ioc\Definition\Source\DefinitionArray;
use Ioc\Definition\Source\ReflectionBasedAutowiring;
use Ioc\Definition\Source\SourceChain;
use Ioc\Proxy\ProxyFactory;

include "../vendor/autoload.php";

class Test
{
    /**
     * @throws \Exception
     */
    public function main(): void
    {


        $dogName = new DogName("test ...");
        $dogName1 = new DogName("test1 ...");
        $dogName2 = new DogName("test2 ...");
//
//        $createDefinitionHelper = new CreateDefinitionHelper(Dog::class);
//        $createDefinitionHelper->constructor($dogName, 123);
//
//        $createDefinitionHelper->property("dogName1", $dogName1);
//        $createDefinitionHelper->property("age1", 111);
//
//
//        $createDefinitionHelper->method("setDogName2", $dogName2);
//        $createDefinitionHelper->method("setAge2", 222);
//        $objectDefinition = $createDefinitionHelper->getDefinition("dog");
//
//
//        // 代理
//        $proxyFactory = new ProxyFactory();
//        $container = new Container();
//        $resolverDispatcher = new ResolverDispatcher($container, $proxyFactory);

//        $objectCreator = new ObjectCreator($resolverDispatcher, $proxyFactory);
//        $object = $objectCreator->resolve($objectDefinition);
//        print_r($object);


//
//        $f = new FactoryDefinition(
//            'test',
//            function ($ioc, $objDef) {
//                print_r($ioc);
//                print_r($objDef);
//            },
//            []
//        );
//
//        $decoratorResolver = new FactoryResolver($container, $resolverDispatcher);
//        $decoratorResolver->resolve($f);

//        $container = new Container;
//        $dog = $container->make(Dog::class, ['dogName' => $dogName1, 'age' => 123]);
//        $container->set(Dog::class, $dog);
//
//       print_r( $container->get(Dog::class));

//        print_r($container->debugEntry(Dog::class));
//
//        print_r($container->getKnownEntryNames());


        //   $make = $container->make(Dog::class, [$dogName, 123]);
        //$make = $container->make(Dog::class, ['dogName' => $dogName, 'age' => 123]);

        // print_r($make);


//        $reflectionBasedAutowiring = new ReflectionBasedAutowiring;
//        $attributeBasedAutowiring = new AttributeBasedAutowiring;
//
//        $definitionArray = new DefinitionArray([],$reflectionBasedAutowiring);
//
//        $sources[] = $reflectionBasedAutowiring;
//        $sources[] = $attributeBasedAutowiring;
//        $sources[] = $definitionArray;
//
//        $source = new SourceChain($sources);
//        $source->setMutableDefinitionSource($definitionArray);
//
//        $compiler = new Compiler(new ProxyFactory());
//        $test = $compiler->compile(
//            $source,
//            './Cache/',
//            'SubContainer',
//            'Container',
//            false
//        );

        //unlink('./Cache/CompiledContainer.php');

//        $containerBuilder = new ContainerBuilder();
//        $containerBuilder->enableCompilation(
//            directory: "./Cache",
//            containerClass: "CompiledContainerCache"
//        );
//
//        $createDefinitionHelper = new CreateDefinitionHelper();
//        $createDefinitionHelper->constructor(123);
//        $dogDef = $createDefinitionHelper->getDefinition(Dog::class);
//        $dogDef->setClassName(Dog::class);
//        $dogDef->setLazy(true);
//
//
//        $definitionArray = new DefinitionArray([
//            Dog::class=>$dogDef
//        ]);
//
//
//
//
//        $containerBuilder->addDefinitions($definitionArray);
//
//        $containerBuilder->useAttributes(true);
//        $containerBuilder->useAutowiring(true);
//        $container1 = $containerBuilder->build();

        //$dog = $container1->make(Dog::class, ['dogName' => $dogName1, 'age' => 123]);


    }
}


$test = new Test();
$test->main();
