<?php


namespace Ioc;
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

        $container = new Container;
        $dog = $container->make(Dog::class,['dogName'=>$dogName1,'age'=>123]);

        $container->set(Dog::class,$dog);

        print_r($container->debugEntry(Dog::class));

        print_r($container->getKnownEntryNames());




     //   $make = $container->make(Dog::class, [$dogName, 123]);
        //$make = $container->make(Dog::class, ['dogName' => $dogName, 'age' => 123]);

       // print_r($make);
    }
}


$test = new Test();
$test->main();
