class <?php echo $this->containerClass; ?> extends <?php echo $this->containerParentClass; ?>
{
const array METHOD_MAPPING = <?php var_export($this->entryToMethodMapping); ?>;

<?php foreach ($this->methods as $methodName => $methodContent) { ?>
    protected function <?php echo $methodName; ?>()
    {
    <?php echo $methodContent; ?>

    }

<?php } ?>
}
