$(document).ready(function () {
    // Select Option on Search Result Page
    $(document).on('click', ".select-option_box img", function () {
        // $('.selectable-option').toggle(500)
        $(this).parents(".select-option-container").find(".selectable-option").toggle(500)
    })

    $(document).on('click', ".selectable-option p", function () {
        var selected_text = $(this).text()

        $(this).parents(".select-option-container").find(".selected-option").text(selected_text)
        $(this).parents(".select-option-container").find('.selectable-option').hide(300)
    })

    var footer_hover_icon_list = [
        './assets/images/icons/icon-mobile-home-white.svg',
        './assets/images/icons/icon-mobile-profle-white.svg',
        './assets/images/icons/icon-mobile-logout-white.svg',
        './assets/images/icons/icon-mobile-menu-white.svg',
    ]

    var footer_icon_list = [
        './assets/images/icons/icon-mobile-home.svg',
        './assets/images/icons/icon-mobile-profle.svg',
        './assets/images/icons/icon-mobile-logout.svg',
        './assets/images/icons/icon-mobile-menu.svg',
    ]

    $(".footer-menu-box .footer-icon").mouseover(function () {
        $(this).find("img").attr("src", footer_hover_icon_list[$(this).index()])
    })

    $(".footer-menu-box .footer-icon").mouseleave(function () {
        $(this).find("img").attr("src", footer_icon_list[$(this).index()])
    })

    $('#mobile-menu-toggle').on('click', function (e) {
        $('.banner-link-box').toggleClass('mobile-menu');
        e.stopPropagation();
        $(document).one('click', function () {
            $('.banner-link-box').removeClass('mobile-menu');
        });
    });
});
